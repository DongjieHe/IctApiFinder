package ict.pag.main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.BriefUnitGraph;

public class APICompatAnalysis {
	private static InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private SetupApplication app;
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private SdkAPIMgr sdkMgr;

	public APICompatAnalysis(String apkPath) {
		String androidJarDir = ConfigMgr.v().getSdkDBDir();
		app = new SetupApplication(androidJarDir, apkPath);
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

		ConcernUnits.v().setUnits(ifStmtSet);

		System.out.println(ifStmtSet.size() + " vs " + apiSet.size());
		for (Iterator<Unit> it = ifStmtSet.iterator(); it.hasNext();) {
			Unit unit = it.next();
			assert unit instanceof JIfStmt;
			JIfStmt ifStmt = (JIfStmt) unit;
			for (Unit mu : icfg.getSuccsOf(unit)) {
				System.out.println("succ of " + unit + " is " + mu + ", " + mu.getJavaSourceStartLineNumber());
			}
			Stmt tgt = ifStmt.getTarget();
			System.out.println(ifStmt + " ; " + tgt);

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
					boolean flag = false;
					if (stmt instanceof JIfStmt) {
						HashSet<Value> flowBefore = simaa.getFlowBefore(u);
						JIfStmt is = (JIfStmt) stmt;
						if (PagHelper.isConcernIfStmt(is, flowBefore)) {
							ifStmtSet.add(u);
							System.out.println(body);
						}
					} else if (stmt instanceof InvokeStmt) {
						InvokeExpr expr = stmt.getInvokeExpr();
						SootMethod callee = expr.getMethod();
						String calleeSig = callee.getSignature();
						flag = sdkMgr.containAPI(calleeSig);
					} else if (stmt instanceof AssignStmt) {
						Value right = ((AssignStmt) stmt).getRightOp();
						if (right instanceof InstanceFieldRef) {
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

}
