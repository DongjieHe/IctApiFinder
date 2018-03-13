package ict.pag.main;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ict.pag.global.ConfigMgr;
import ict.pag.utils.PagHelper;
import soot.Body;
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
import soot.toolkits.graph.BriefUnitGraph;

@Deprecated
class PotentialEntity {
	boolean[] record;
	SootMethod mSm;
	Stmt mStmt;

	PotentialEntity(SootMethod sm, Stmt stmt) {
		record = new boolean[28];
		mStmt = stmt;
		mSm = sm;
		for (int i = 0; i < 28; ++i) {
			record[i] = false;
		}
	}

	public void setRecord(int idx, boolean flag) {
		record[idx] = flag;
	}

	public void dump() {
		System.out.println("=================DUMP BEGIN===============");
		System.out.println(mSm.getActiveBody());
		System.out.println("==========================================");
		System.out.println(mStmt);
		int beg = 0;
		boolean curr = record[0];
		for (int i = 1; i < 28; ++i) {
			if (i == 20) {
				continue;
			}
			if (record[i] != curr) {
				System.out.println("<" + beg + "," + (i - 1) + ">:" + curr);
				curr = record[i];
				beg = i;
			}
		}
		System.out.println("<" + beg + ",27>:" + curr);
		System.out.println("==================DUMP END================");
	}
}

public class Main {
	/**
	 * This method collect IfStmt Set and api used in APK file.
	 *
	 * @param apkPath:
	 *            where the apk file is.
	 * @param sdkMgr:
	 *            contain all SDKs
	 * @param ifStmtSet:
	 *            all IfStmt relate to <android.os.Build$VERSION: int SDK_INT>
	 * @param apiSet:
	 *            all stmt that use an API from SDK.
	 * @throws Exception
	 */
	public static void preAnalysis(String apkPath, SdkAPIMgr sdkMgr, Set<Unit> ifStmtSet, Set<Unit> apiSet)
			throws Exception {
		Set<SootClass> apkClasses = PagHelper.loadAPKClasses(apkPath);
		for (Iterator<SootClass> it = apkClasses.iterator(); it.hasNext();) {
			SootClass sc = it.next();
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
					if (stmt instanceof IfStmt) {
						HashSet<Value> flowBefore = simaa.getFlowBefore(u);
						IfStmt is = (IfStmt) stmt;
						if (PagHelper.isConcernIfStmt(is, flowBefore)) {
							ifStmtSet.add(u);
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

	// need to know which stmt in which method, and api set which there are likely to be visit.
	public static void analysze(String apkPath, SdkAPIMgr sdkMgr, Set<Unit> ifStmtSet, Set<Unit> apiSet)
			throws Exception {
		// !TODO;
	}

	public static void check() {
		// !TODO;
	}

	public static void main(String[] args) {
		ConfigMgr cm = ConfigMgr.v();
		System.out.println(cm.getMaxSdkVersion());
		System.out.println(cm.getMinSdkVersion());
		System.out.println(cm.getOutputDir());
		System.out.println(cm.getSdkDBDir());
		System.out.println(cm.getTestSetDir());

		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		sdkMgr.dump();

		String apkPath = "/home/hedj/Work/android/fdroidNewest/android.game.prboom_31.apk";
		Set<Unit> ifStmtSet = new HashSet<Unit>();
		Set<Unit> apiSet = new HashSet<Unit>();
		try {
			preAnalysis(apkPath, sdkMgr, ifStmtSet, apiSet);
			analysze(apkPath, sdkMgr, ifStmtSet, apiSet);
			check();
			System.out.println("minSDKVersion: " + PagHelper.getMinSdkVersion(apkPath));
			System.out.println("targetSDKVersion: " + PagHelper.getTargetSdkVersion(apkPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
