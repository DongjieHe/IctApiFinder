package ict.pag.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.IFDSTabulationProblem;
import ict.pag.core.FinderFact;
import ict.pag.core.FinderProblem;
import ict.pag.core.FinderSolver;
import ict.pag.core.SdkIntMustAliasAnalysis;
import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConcernUnits;
import ict.pag.global.ConfigMgr;
import ict.pag.utils.CodeTimer;
import ict.pag.utils.PagHelper;
import soot.Body;
import soot.BooleanType;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.BriefUnitGraph;

public class IctApiFinder {
	private static InfoflowAndroidConfiguration config;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private AndroidApplication app;
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private SdkAPIMgr sdkMgr;
	private CodeTimer codeTimer;

	public IctApiFinder(String apkPath) {
		config = new InfoflowAndroidConfiguration();
		config.setUseExistingSootInstance(false);
		String androidJarDir = ConfigMgr.v().getSdkDBDir();
		app = new AndroidApplication(androidJarDir, apkPath);
		app.setConfig(config);
		sdkMgr = null;
		icfg = null;
		codeTimer = new CodeTimer();
	}

	public void setSdkMgr(SdkAPIMgr sdkMgr) {
		this.sdkMgr = sdkMgr;
	}

	// need to know which stmt in which method, and api set which there are likely to be visit.
	public StatUnit runAnalysis(boolean fullDetail) {
		logger.info("Start analysis " + app.getAppName() + "!");
		logger.info("start constructing call graph for " + app.getAppName());
		StatUnit su = new StatUnit();

		codeTimer.startTimer();
		app.constructCallgraph();
		icfg = new JimpleBasedInterproceduralCFG(config.getEnableExceptionTracking(), true);
		IFDSTabulationProblem<Unit, FinderFact, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> finderProblem = new FinderProblem(
				icfg);
		codeTimer.stopTimer();
		su.setIcfg(codeTimer.getExecutionTime());
		logger.info("finish building icfg for " + app.getAppName());
		// ((FinderProblem) finderProblem).setThreadsNum(1);

		logger.info("Running pre-analysis...");
		codeTimer.startTimer();
		Map<Unit, Set<Integer>> ifStmt2Killing = new HashMap<Unit, Set<Integer>>();
		Set<Unit> apiSet = new HashSet<Unit>();
		try {
			preAnalysis(ifStmt2Killing, apiSet);
		} catch (Exception e) {
			logger.error("fail to pre-analysis " + app.getAppName() + "!!!");
			e.printStackTrace();
		}
		ConcernUnits.v().setUnitToKillMap(ifStmt2Killing);
		ConcernUnits.v().setApiSet(apiSet);
		codeTimer.stopTimer();
		su.setPreAna(codeTimer.getExecutionTime());

		logger.info("Setting initial seeds...");
		codeTimer.startTimer();
		Set<FinderFact> initialSeeds = new HashSet<FinderFact>();
		for (int i = ConfigMgr.v().getMinSdkVersion(); i <= ConfigMgr.v().getMaxSdkVersion(); ++i) {
			FinderFact finderFact = new FinderFact(i);
			initialSeeds.add(finderFact);
		}

		List<SootMethod> entryPoints = Scene.v().getEntryPoints();
		for (SootMethod sm : entryPoints) {
			Collection<Unit> startPoints = icfg.getStartPointsOf(sm);
			for (Unit u : startPoints) {
				((FinderProblem) finderProblem).addInitialSeeds(u, initialSeeds);
				logger.info("entry: " + u);
			}
		}

		logger.info("Running data flow analysis...");
		FinderSolver finderSolver = new FinderSolver(finderProblem);
		finderSolver.setEnableMergePointChecking(true);
		finderSolver.solve();
		codeTimer.stopTimer();
		su.setIfds(codeTimer.getExecutionTime());

		logger.info("Checking API Use compatibility...");
		codeTimer.startTimer();
		int bugNum = -1;
		try {
			bugNum = checkAPICompatibility(fullDetail);
		} catch (Exception e) {
			logger.error("check API Compatibility in " + app.getAppName() + " failed!");
			e.printStackTrace();
		}
		codeTimer.stopTimer();
		su.setCheckComp(codeTimer.getExecutionTime());
		su.setBugNum(bugNum);
		logger.info("finish analysis " + app.getAppName() + "!");
		finderSolver = null;
		finderProblem = null;

		return su;
	}

	/**
	 * This method collect IfStmt Set and api used in APK file.
	 *
	 * @param ifStmtSet:
	 *            all IfStmt relate to <android.os.Build$VERSION: int SDK_INT>
	 * @param apiSet:
	 *            all stmt that use an API from SDK.
	 * @throws Exception
	 */
	private void preAnalysis(Map<Unit, Set<Integer>> ifStmt2Killing, Set<Unit> apiSet) throws Exception {
		Map<Unit, Set<Integer>> bool2killing = new HashMap<Unit, Set<Integer>>();
		Map<SootMethod, Set<Integer>> sm2killing = new HashMap<SootMethod, Set<Integer>>();
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.getName().startsWith("android.support")) {
				continue;
			}
			List<SootMethod> sms = sc.getMethods();
			for (SootMethod sm : sms) {
				if (!sm.isConcrete() || !sm.hasActiveBody()) {
					continue;
				}
				Body body = sm.getActiveBody();
				BriefUnitGraph noBug = new BriefUnitGraph(body);
				SdkIntMustAliasAnalysis simaa = new SdkIntMustAliasAnalysis(noBug);

				for (Unit u : body.getUnits()) {
					Stmt stmt = (Stmt) u;
					HashSet<Value> flowBefore = simaa.getFlowBefore(u);
					// collect concern ifStmt.
					if (stmt instanceof JIfStmt) {
						JIfStmt is = (JIfStmt) stmt;
						Value cond = is.getCondition();
						if (PagHelper.isConcernExpr(cond, flowBefore)) {
							Set<Integer> killSet = PagHelper.fetchKillingSet(cond);
							ifStmt2Killing.put(u, killSet);
						}
					}
					// collect SDK API
					boolean flag = false;
					if (stmt instanceof InvokeStmt) {
						InvokeExpr expr = ((InvokeStmt) stmt).getInvokeExpr();
						SootMethod callee = expr.getMethod();
						flag = sdkMgr.containAPI(callee);
					} else if (stmt instanceof AssignStmt) {
						Value right = ((AssignStmt) stmt).getRightOp();
						if (right instanceof InvokeExpr) {
							InvokeExpr expr = (InvokeExpr) right;
							flag = sdkMgr.containAPI(expr.getMethod());
						} else if (right instanceof InstanceFieldRef) {
							InstanceFieldRef ref = (InstanceFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = sdkMgr.containAPI(fieldSig);
						} else if (right instanceof StaticFieldRef) {
							StaticFieldRef ref = (StaticFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = sdkMgr.containAPI(fieldSig);
						}

						// collect concern bool expr;
						if (PagHelper.isConcernExpr(right, flowBefore)) {
							bool2killing.put(u, PagHelper.fetchKillingSet(right));
						}
					}
					if (flag) {
						apiSet.add(u);
					}

				} // for unit
					// collect boolean {return SDK_INT op CNT;} methods.
				BriefBlockGraph bg = new BriefBlockGraph(body);
				// DotGraph dg = (new CFGToDotGraph()).drawCFG(bg, body);
				// dg.plot(sm.getSignature() + ".dot");
				updateIfConcernReturnMethod(bg, ifStmt2Killing, sm2killing);
			} // for method
		} // for class

		logger.info("finish first-round pre-analysis...");
		/**
		 * the second time to traverse the whole apk classes. mainly collect IfStmt that with condition expr of concern
		 * boolean or returnMethod.
		 */
		Map<SootMethod, Set<Value>> sm2vs = new HashMap<SootMethod, Set<Value>>();
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			List<SootMethod> sms = sc.getMethods();
			for (SootMethod sm : sms) {
				if (!sm.isConcrete()) {
					continue;
				}
				sm.retrieveActiveBody();
				Body body = sm.getActiveBody();
				for (Unit u : body.getUnits()) {
					Stmt stmt = (Stmt) u;
					if (stmt instanceof AssignStmt) {
						Value left = ((AssignStmt) stmt).getLeftOp();
						Value right = ((AssignStmt) stmt).getRightOp();
						if (right instanceof InvokeExpr) {
							InvokeExpr expr = (InvokeExpr) right;
							SootMethod callee = expr.getMethod();
							if (sm2killing.containsKey(callee)) {
								if (sm2vs.containsKey(callee)) {
									sm2vs.get(callee).add(left);
								} else {
									Set<Value> tmp = new HashSet<Value>();
									tmp.add(left);
									sm2vs.put(callee, tmp);
								}
							}
						}
					}
					// collect concern ifStmt.
					if (stmt instanceof JIfStmt) {
						JIfStmt is = (JIfStmt) stmt;
						Value cond = is.getCondition();
						if (cond instanceof JEqExpr) {
							JEqExpr eqExpr = (JEqExpr) cond;
							Value op1 = eqExpr.getOp1();
							Value op2 = eqExpr.getOp2();
							if (op1 instanceof IntConstant) {
								Value tmp = op1;
								op1 = op2;
								op2 = tmp;
							}
							// Map<SootMethod, Set<Value>> sm2vs;
							for (Entry<SootMethod, Set<Value>> entry : sm2vs.entrySet()) {
								if (entry.getValue().contains(op1)) {
									Set<Integer> kSet = sm2killing.get(entry.getKey());
									Set<Integer> finalSet;
									if (((IntConstant) op2).value == 1) {
										finalSet = kSet;
									} else {
										int mMinVersion = ConfigMgr.v().getMinSdkVersion();
										int mMaxVersion = ConfigMgr.v().getMaxSdkVersion();
										finalSet = new HashSet<Integer>();
										for (int i = mMinVersion; i <= mMaxVersion; ++i) {
											if (!kSet.contains(i)) {
												finalSet.add(i);
											}
										}
									}
									ifStmt2Killing.put(u, finalSet);
								}
							}
						}
						// if(bool2killing.containsKey(cond)) {
						// Set<Integer> killSet = bool2killing.get(cond);
						// ifStmt2Killing.put(u, killSet);
						// }
					}
				} // for unit
			} // for method
		} // for class
	}

	/**
	 * check whether is our concern return method.
	 */
	private boolean updateIfConcernReturnMethod(BriefBlockGraph bg, Map<Unit, Set<Integer>> if2kill,
			Map<SootMethod, Set<Integer>> sm2killing) {
		SootMethod sm = bg.getBody().getMethod();
		Type mType = sm.getReturnType();
		boolean flag = mType instanceof BooleanType;
		flag &= bg.getBlocks().size() == 3;
		flag &= bg.getHeads().size() == 1;
		if (flag == false) {
			return false;
		}
		Block head = bg.getHeads().get(0);
		Unit tail = head.getTail();
		List<Block> succBlks = head.getSuccs();
		if (succBlks.size() != 2 || !if2kill.containsKey(tail)) {
			return false;
		}

		JIfStmt is = (JIfStmt) tail;
		Stmt tgt = is.getTarget();
		Unit succTail1 = succBlks.get(0).getTail();
		Unit succTail2 = succBlks.get(1).getTail();
		if (!(succTail1 instanceof JReturnStmt) || !(succTail2 instanceof JReturnStmt)) {
			return false;
		}
		JReturnStmt jrStmt1 = (JReturnStmt) succTail1;
		JReturnStmt jrStmt2 = (JReturnStmt) succTail2;
		Value op1 = jrStmt1.getOp();
		Value op2 = jrStmt2.getOp();
		if (!(op1 instanceof IntConstant) || !(op2 instanceof IntConstant)) {
			return false;
		}
		IntConstant ic1 = (IntConstant) op1;
		IntConstant ic2 = (IntConstant) op2;
		Set<Integer> killing = PagHelper.fetchKillingSet(is.getCondition());
		if (tgt == succTail1 && ic1.value == 1 || tgt == succTail1 && ic2.value == 0) {
			sm2killing.put(sm, killing);
		} else {
			int mMinVersion = ConfigMgr.v().getMinSdkVersion();
			int mMaxVersion = ConfigMgr.v().getMaxSdkVersion();
			Set<Integer> mKilling = new HashSet<Integer>();
			for (int i = mMinVersion; i <= mMaxVersion; ++i) {
				if (!killing.contains(i)) {
					mKilling.add(i);
				}
			}
			sm2killing.put(sm, mKilling);
		}
		return true;
	}

	/**
	 * Check whether this app contain API incompatibility problem.
	 */
	private int checkAPICompatibility(boolean fullDetail) throws Exception {
		Map<Unit, Set<Integer>> api2live = ConcernUnits.v().getApi2live();
		Set<BugUnit> bugReport = new HashSet<BugUnit>();
		int minSdkVersion = app.getMinSdkVersion();
		int maxSdkVersion = app.targetSdkVersion();
		for (Entry<Unit, Set<Integer>> entry : api2live.entrySet()) {
			Unit key = entry.getKey();
			Set<Integer> liveLevels = entry.getValue();
			// filter api level;
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (level > maxSdkVersion || level < minSdkVersion) {
					it.remove();
				}
			}

			if (key instanceof InvokeStmt) {
				InvokeStmt ivk = (InvokeStmt) key;
				InvokeExpr expr = ivk.getInvokeExpr();
				SootMethod callee = expr.getMethod();
				if (callee.getDeclaringClass().isApplicationClass()) {
					continue;
				}
				collectMethodAPIBug(key, callee, liveLevels, bugReport);
			} else if (key instanceof AssignStmt) {
				Value right = ((AssignStmt) key).getRightOp();
				if (right instanceof InvokeExpr) {
					InvokeExpr expr = (InvokeExpr) right;
					SootMethod callee = expr.getMethod();
					if (callee.getDeclaringClass().isApplicationClass()) {
						continue;
					}
					collectMethodAPIBug(key, callee, liveLevels, bugReport);
				} else if (right instanceof InstanceFieldRef) {
					InstanceFieldRef ref = (InstanceFieldRef) right;
					SootField sf = ref.getField();
					if (sf.getDeclaringClass().isApplicationClass()) {
						continue;
					}
					String fieldSig = sf.getSignature();
					collectFieldAPIBug(key, fieldSig, liveLevels, bugReport);
				} else if (right instanceof StaticFieldRef) {
					StaticFieldRef ref = (StaticFieldRef) right;
					SootField sf = ref.getField();
					if (sf.getDeclaringClass().isApplicationClass()) {
						continue;
					}
					String fieldSig = sf.getSignature();
					collectFieldAPIBug(key, fieldSig, liveLevels, bugReport);
				} else {
					logger.error("should not collect " + key + " in pre-analysis phase: " + key.getClass());
				}

			} else {
				logger.error("should not collect " + key + " in pre-analysis phase: " + key.getClass());
			}
		}
		// report bugs
		if (bugReport.size() == 0) {
			logger.info("no potential bugs!");
			return 0;
		}
		String outDir = ConfigMgr.v().getOutputDir();
		String reportFile = outDir + File.separator + app.getAppName() + ".report";
		FileOutputStream fos = new FileOutputStream(reportFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		bw.write(app.getAppName() + " minSdkVersion: " + minSdkVersion + ", targetSdkVersion: " + maxSdkVersion);
		bw.newLine();
		bw.flush();
		for (Iterator<BugUnit> it = bugReport.iterator(); it.hasNext();) {
			BugUnit bugMsg = it.next();
			if (fullDetail && bugMsg.getBugType() == 0) {
				continue;
			}
			bw.write(bugMsg.toString(fullDetail));
			bw.newLine();
		}
		bw.close();
		return bugReport.size();
	}

	private void collectMethodAPIBug(Unit callSite, SootMethod callee, Set<Integer> liveLevels,
			Set<BugUnit> bugReport) {
		if (!icfg.isReachable(callSite)) {
			return;
		}
		// !FIXME I don't know support method is here!
		if (callee.getDeclaringClass().getName().startsWith("android.support")) {
			return;
		}
		SootMethod sm = icfg.getMethodOf(callSite);
		int row = callSite.getJavaSourceStartLineNumber();
		String calleeSig = callee.getSignature();
		boolean isApplication = sm.getDeclaringClass().isApplicationClass();
		assert isApplication;
		String callerSig = sm.getSignature();
		PathTracer tracer = new PathTracer(icfg, callSite);
		if (liveLevels.size() == 0) {
			String bugMsg = calleeSig + " called in " + callerSig + " on line " + row + " no living Level";
			BugUnit bug = new BugUnit(bugMsg, tracer.getAllPossibleCallStack(), 0);
			// bugReport.add(bug);
		} else {
			Set<Integer> missing = new HashSet<Integer>();
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (!sdkMgr.containMethodAPI(level, callee)) {
					missing.add(level);
				}
			}
			if (missing.size() > 0) {
				tracer.trace();
				String bugMsg = calleeSig + " called in " + callerSig + " on line " + row + " not in " + missing;
				BugUnit bug = new BugUnit(bugMsg, tracer.getAllPossibleCallStack(), 1);
				bugReport.add(bug);
			}
		}
	}

	private void collectFieldAPIBug(Unit unit, String fieldSig, Set<Integer> liveLevels, Set<BugUnit> bugReport) {
		if (!icfg.isReachable(unit)) {
			return;
		}
		SootMethod sm = icfg.getMethodOf(unit);
		boolean isApplication = sm.getDeclaringClass().isApplicationClass();
		assert isApplication;
		String callerSig = sm.getSignature();
		int row = unit.getJavaSourceStartLineNumber();
		PathTracer tracer = new PathTracer(icfg, unit);
		if (liveLevels.size() == 0) {
			String bugMsg = fieldSig + " called in " + callerSig + " on line " + row + " no living Level";
			BugUnit bug = new BugUnit(bugMsg, tracer.getAllPossibleCallStack(), 0);
			// bugReport.add(bug);
		} else {
			Set<Integer> missing = new HashSet<Integer>();
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (!sdkMgr.containFieldAPI(level, fieldSig)) {
					missing.add(level);
				}
			}
			if (missing.size() > 0) {
				tracer.trace();
				String bugMsg = fieldSig + " called in " + callerSig + " on line " + row + " not in " + missing;
				BugUnit bug = new BugUnit(bugMsg, tracer.getAllPossibleCallStack(), 1);
				bugReport.add(bug);
			}

		}
	}

	public void releaseCallgraph() {
		Scene.v().releaseCallGraph();
		Scene.v().releasePointsToAnalysis();
		Scene.v().releaseReachableMethods();
		G.v().resetSpark();
	}

}
