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
import soot.Scene;
import soot.SootClass;
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
	private static InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private AndroidApplication app;
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private SdkAPIMgr sdkMgr;

	public APICompatAnalysis(String apkPath) {
		String androidJarDir = ConfigMgr.v().getSdkDBDir();
		app = new AndroidApplication(androidJarDir, apkPath);
		app.setConfig(config);
		app.constructCallgraph();
		icfg = new JimpleBasedInterproceduralCFG(config.getEnableExceptionTracking(), true);
		sdkMgr = null;

	}

	public void setSdkMgr(SdkAPIMgr sdkMgr) {
		this.sdkMgr = sdkMgr;
	}

	// need to know which stmt in which method, and api set which there are likely to be visit.
	public void runAnalysis() {
		Set<Unit> ifStmtSet = new HashSet<Unit>();
		Set<Unit> apiSet = new HashSet<Unit>();
		logger.info("Running pre-analysis...");
		try {
			preAnalysis(ifStmtSet, apiSet);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Map<Unit, Set<Integer>> retMap = createUnitToKillingMap(ifStmtSet);
		ConcernUnits.v().setUnitToKillMap(retMap);
		ConcernUnits.v().setApiSet(apiSet);

		logger.info("Setting initial seeds...");
		IFDSTabulationProblem<Unit, FinderFact, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> finderProblem = new FinderProblem(
				icfg);
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
				System.out.println("entry: " + u);
			}
		}

		logger.info("Running data flow analysis...");
		FinderSolver finderSolver = new FinderSolver(finderProblem);
		finderSolver.solve();
		logger.info("finishing analysis!");
		System.out.println(ifStmtSet.size() + " vs " + apiSet.size());
		logger.info("Checking API Use compatibility...");
		try {
			checkAPICompatibility();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		String outDir = ConfigMgr.v().getOutputDir();
		String reportFile = outDir + File.separator + app.getAppName() + ".report";
		FileOutputStream fos = new FileOutputStream(reportFile);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
		int minSdkVersion = app.getMinSdkVersion();
		int maxSdkVersion = app.targetSdkVersion();
		bw.write(app.getAppName() + " minSdkVersion: " + minSdkVersion + ", maxSdkVersion: " + maxSdkVersion);
		bw.newLine();
		bw.flush();
		for (Entry<Unit, Set<Integer>> entry : api2live.entrySet()) {
			Unit key = entry.getKey();
			Set<Integer> liveLevels = entry.getValue();
			// get calleeSig
			String calleeSig = null;
			if (key instanceof InvokeStmt) {
				InvokeStmt ivk = (InvokeStmt) key;
				InvokeExpr expr = ivk.getInvokeExpr();
				calleeSig = expr.getMethod().getSignature();
			} else if (key instanceof AssignStmt) {
				Value right = ((AssignStmt) key).getRightOp();
				assert right instanceof InvokeExpr;
				InvokeExpr expr = (InvokeExpr) right;
				calleeSig = expr.getMethod().getSignature();
			} else {
				logger.error("callsite other stmt: " + entry.getKey().getClass().toString());
			}
			assert calleeSig != null;
			// filter api level;
			for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
				int level = it.next();
				if (level > maxSdkVersion || level < minSdkVersion) {
					it.remove();
				}
			}
			// check and report
			if (liveLevels.size() == 0) {
				bw.write(calleeSig + " not live in any API Level but be called in code!");
				bw.newLine();
			} else {
				Set<Integer> missing = new HashSet<Integer>();
				for (Iterator<Integer> it = liveLevels.iterator(); it.hasNext();) {
					int level = it.next();
					if (!sdkMgr.containAPI(level, calleeSig)) {
						missing.add(level);
					}
				}
				if (missing.size() > 0) {
					bw.write(calleeSig + " missing in " + missing);
					bw.newLine();
				}
			}
		}
		bw.close();
	}
}
