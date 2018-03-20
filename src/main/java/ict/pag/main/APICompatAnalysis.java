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
import ict.pag.utils.PagHelper;
import soot.Body;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;

public class APICompatAnalysis {
	private static InfoflowAndroidConfiguration config;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private AndroidApplication app;
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private SdkAPIMgr sdkMgr;

	public APICompatAnalysis(String apkPath) {
		config = new InfoflowAndroidConfiguration();
		config.setUseExistingSootInstance(false);
		String androidJarDir = ConfigMgr.v().getSdkDBDir();
		app = new AndroidApplication(androidJarDir, apkPath);
		app.setConfig(config);
		sdkMgr = null;
		icfg = null;
	}

	public void setSdkMgr(SdkAPIMgr sdkMgr) {
		this.sdkMgr = sdkMgr;
	}

	// need to know which stmt in which method, and api set which there are likely to be visit.
	public void runAnalysis() {
		logger.info("Start analysis " + app.getAppName() + "!");
		app.constructCallgraph();
		logger.info("finish build call graph for " + app.getAppName());
		Set<Unit> ifStmtSet = new HashSet<Unit>();
		Set<Unit> apiSet = new HashSet<Unit>();
		logger.info("Running pre-analysis...");
		try {
			preAnalysis(ifStmtSet, apiSet);
		} catch (Exception e) {
			System.err.println("fail to pre-analysis " + app.getAppName() + "!!!");
			e.printStackTrace();
		}

		Map<Unit, Set<Integer>> retMap = createUnitToKillingMap(ifStmtSet);
		ConcernUnits.v().setUnitToKillMap(retMap);
		ConcernUnits.v().setApiSet(apiSet);

		logger.info("Setting initial seeds...");
		icfg = new JimpleBasedInterproceduralCFG(config.getEnableExceptionTracking(), true);
		IFDSTabulationProblem<Unit, FinderFact, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> finderProblem = new FinderProblem(
				icfg);
		((FinderProblem) finderProblem).setThreadsNum(1);
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
		finderSolver = null;
		finderProblem = null;
		System.out.println(ifStmtSet.size() + " vs " + apiSet.size());
		logger.info("Checking API Use compatibility...");
		try {
			checkAPICompatibility();
		} catch (Exception e) {
			System.err.println("check API Compatibility in " + app.getAppName() + " failed!");
			e.printStackTrace();
		}
		logger.info("finish analysis " + app.getAppName() + "!");
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
	private void preAnalysis(Set<Unit> ifStmtSet, Set<Unit> apiSet) throws Exception {
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			List<SootMethod> sms = sc.getMethods();
			for (SootMethod sm : sms) {
				if (!sm.isConcrete()) {
					continue;
				}
				sm.retrieveActiveBody();
				Body body = sm.getActiveBody();
				BriefUnitGraph noBug = new BriefUnitGraph(body);
				SdkIntMustAliasAnalysis simaa = new SdkIntMustAliasAnalysis(noBug);
				for (Unit u : body.getUnits()) {
					Stmt stmt = (Stmt) u;
					// collect concern ifStmt.
					if (stmt instanceof JIfStmt) {
						HashSet<Value> flowBefore = simaa.getFlowBefore(u);
						JIfStmt is = (JIfStmt) stmt;
						if (PagHelper.isConcernIfStmt(is, flowBefore)) {
							ifStmtSet.add(u);
						}
					}
					// collect SDK API
					boolean flag = false;
					if (stmt instanceof InvokeStmt) {
						InvokeExpr expr = stmt.getInvokeExpr();
						SootMethod callee = expr.getMethod();
						String calleeSig = callee.getSignature();
						flag = sdkMgr.containAPI(calleeSig);
					} else if (stmt instanceof AssignStmt) {
						Value right = ((AssignStmt) stmt).getRightOp();
						if (right instanceof InvokeExpr) {
							InvokeExpr expr = (InvokeExpr) right;
							String calleeSig = expr.getMethod().getSignature();
							flag = sdkMgr.containAPI(calleeSig);
						} else if (right instanceof InstanceFieldRef) {
							InstanceFieldRef ref = (InstanceFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = sdkMgr.containAPI(fieldSig);
						} else if (right instanceof StaticFieldRef) {
							StaticFieldRef ref = (StaticFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = sdkMgr.containAPI(fieldSig);
						}
					}
					if (flag) {
						apiSet.add(u);
					}

				} // for unit
			} // for method
		} // for class
	}

	private Map<Unit, Set<Integer>> createUnitToKillingMap(Set<Unit> ifStmtSet) {
		Map<Unit, Set<Integer>> retMap = new HashMap<Unit, Set<Integer>>();
		for (Iterator<Unit> it = ifStmtSet.iterator(); it.hasNext();) {
			Unit u = it.next();
			Set<Integer> killSet = PagHelper.fetchKillingSet((IfStmt) u);
			retMap.put(u, killSet);
			logger.info("ifstmt: " + u + "-->" + killSet);
		}
		return retMap;
	}

	/**
	 * Check whether this app contain API incompatibility problem.
	 */
	private void checkAPICompatibility() throws Exception {
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
			// get callee
			SootMethod callee = null;
			if (key instanceof InvokeStmt) {
				InvokeStmt ivk = (InvokeStmt) key;
				InvokeExpr expr = ivk.getInvokeExpr();
				callee = expr.getMethod();
				if (callee.getDeclaringClass().isApplicationClass()) {
					continue;
				}
				collectMethodAPIBug(key, callee, liveLevels, bugReport);
			} else if (key instanceof AssignStmt) {
				Value right = ((AssignStmt) key).getRightOp();
				if (right instanceof InvokeExpr) {
					InvokeExpr expr = (InvokeExpr) right;
					callee = expr.getMethod();
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
			return;
		}
		String outDir = ConfigMgr.v().getOutputDir();
		String reportFile = outDir + File.separator + app.getAppName() + ".report";
		FileOutputStream fos = new FileOutputStream(reportFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		bw.write(app.getAppName() + " minSdkVersion: " + minSdkVersion + ", maxSdkVersion: " + maxSdkVersion);
		bw.newLine();
		bw.flush();
		for (Iterator<BugUnit> it = bugReport.iterator(); it.hasNext();) {
			BugUnit bugMsg = it.next();
			bw.write(bugMsg.toString());
			bw.newLine();
		}
		bw.close();
	}

	private void collectMethodAPIBug(Unit callSite, SootMethod callee, Set<Integer> liveLevels,
			Set<BugUnit> bugReport) {
		int row = callSite.getJavaSourceStartLineNumber();
		int col = callSite.getJavaSourceStartColumnNumber();
		String calleeSig = callee.getSignature();
		SootMethod sm = icfg.getMethodOf(callSite);
		boolean isApplication = sm.getDeclaringClass().isApplicationClass();
		assert isApplication;
		String callerSig = sm.getSignature();
		PathTracer tracer = new PathTracer(icfg, callSite);
		tracer.trace();
		if (liveLevels.size() == 0) {
			String bugMsg = calleeSig + " called in " + callerSig + "<" + row + ", " + col + "> no living Level";
			BugUnit bug = new BugUnit(bugMsg, tracer.getCallStackPath());
			bugReport.add(bug);
		} else {
			Set<Integer> missing = new HashSet<Integer>();
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (!sdkMgr.containMethodAPI(level, callee)) {
					missing.add(level);
				}
			}
			if (missing.size() > 0) {
				String bugMsg = calleeSig + " called in " + callerSig + "<" + row + ", " + col + "> " + " not in "
						+ missing;
				BugUnit bug = new BugUnit(bugMsg, tracer.getCallStackPath());
				bugReport.add(bug);
			}
		}
	}

	private void collectFieldAPIBug(Unit unit, String fieldSig, Set<Integer> liveLevels, Set<BugUnit> bugReport) {
		SootMethod sm = icfg.getMethodOf(unit);
		boolean isApplication = sm.getDeclaringClass().isApplicationClass();
		assert isApplication;
		String callerSig = sm.getSignature();
		int row = unit.getJavaSourceStartLineNumber();
		int col = unit.getJavaSourceStartColumnNumber();
		PathTracer tracer = new PathTracer(icfg, unit);
		tracer.trace();
		if (liveLevels.size() == 0) {
			String bugMsg = fieldSig + " called in " + callerSig + "<" + row + ", " + col + "> no living Level";
			BugUnit bug = new BugUnit(bugMsg, tracer.getCallStackPath());
			bugReport.add(bug);
		} else {
			Set<Integer> missing = new HashSet<Integer>();
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (!sdkMgr.containFieldAPI(level, fieldSig)) {
					missing.add(level);
				}
			}
			if (missing.size() > 0) {
				String bugMsg = fieldSig + " called in " + callerSig + "<" + row + ", " + col + "> " + " not in "
						+ missing;
				BugUnit bug = new BugUnit(bugMsg, tracer.getCallStackPath());
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
