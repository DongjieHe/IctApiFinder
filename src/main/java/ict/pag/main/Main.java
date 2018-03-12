package ict.pag.main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ict.pag.datalog.Executor;
import ict.pag.datalog.LBWorkspaceConnector;
import ict.pag.global.ConfigMgr;
import ict.pag.utils.PagHelper;
import soot.Body;
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

public class Main {

	private static List<SdkAPIs> locadSdkAPIs() {
		ConfigMgr cm = ConfigMgr.v();
		int mMin = cm.getMinSdkVersion();
		int mMax = cm.getMaxSdkVersion();
		assert mMin <= mMax;
		String sdkDir = cm.getSdkDBDir();
		List<SdkAPIs> sdkList = new ArrayList<SdkAPIs>();
		Executor executor = new Executor();
		for (int i = mMin; i <= mMax; ++i) {
			if (i == 20) {
				continue; // api level 20 is a special case.
			}
			String sdkPath = sdkDir + File.separator + i;
			LBWorkspaceConnector conn = new LBWorkspaceConnector(executor, sdkPath, i);
			conn.connect("database");
			SdkAPIs tmp = new SdkAPIs(conn);
			sdkList.add(tmp);
		}
		return sdkList;
	}

	private static boolean isPotential(String apiSig, List<SdkAPIs> sdkList, int apiType) {
		boolean allIn = true, allnotIn = true;
		for (int i = 0; i < sdkList.size(); ++i) {
			SdkAPIs sdkAPI = sdkList.get(i);
			if (apiType == 0) {
				allIn &= sdkAPI.containType(apiSig);
				allnotIn &= !sdkAPI.containType(apiSig);
			} else if (apiType == 1) {
				allIn &= sdkAPI.containMethod(apiSig);
				allnotIn &= !sdkAPI.containMethod(apiSig);
			} else if (apiType == 2) {
				allIn &= sdkAPI.containField(apiSig);
				allnotIn &= !sdkAPI.containMethod(apiSig);
			} else {
				assert false;
			}
		}
		if (allIn || allnotIn) {
			return false;
		}
		return true;
	}

	private static Set<Stmt> collectPotentialUnCompatStmt(Set<SootClass> apkClasses, List<SdkAPIs> sdkList) {
		Set<Stmt> retSet = new HashSet<Stmt>();
		for (Iterator<SootClass> it = apkClasses.iterator(); it.hasNext();) {
			SootClass sc = it.next();
			if (sc.getJavaPackageName().startsWith("android.support")) {
				continue;
			}
			List<SootMethod> sms = sc.getMethods();
			for (SootMethod sm : sms) {
				if (!sm.isConcrete()) {
					continue;
				}
				sm.retrieveActiveBody();
				Body body = sm.getActiveBody();
				for (Unit u : body.getUnits()) {
					Stmt stmt = (Stmt) u;
					boolean flag = false;
					if (stmt instanceof InvokeStmt) {
						InvokeExpr expr = stmt.getInvokeExpr();
						SootMethod callee = expr.getMethod();
						String calleeSig = callee.getSignature();
						flag = isPotential(calleeSig, sdkList, 1);
					} else if (stmt instanceof AssignStmt) {
						Value right = ((AssignStmt) stmt).getRightOp();
						if (right instanceof InstanceFieldRef) {
							InstanceFieldRef ref = (InstanceFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = isPotential(fieldSig, sdkList, 2);
						} else if (right instanceof StaticFieldRef) {
							StaticFieldRef ref = (StaticFieldRef) right;
							String fieldSig = ref.getField().getSignature();
							flag = isPotential(fieldSig, sdkList, 2);
						}
					}
					if (flag) {
						retSet.add(stmt);
					}
				} // for unit
			} // for method
		} // for class
		return retSet;
	}

	public static void analysze(String apkPath, List<SdkAPIs> sdkList) throws Exception {
		Set<SootClass> apkClasses = PagHelper.loadAPKClasses(apkPath);
		Set<Stmt> potentialStmts = collectPotentialUnCompatStmt(apkClasses, sdkList);
		System.out.println(potentialStmts.size());
	}

	public static void main(String[] args) {
		ConfigMgr cm = ConfigMgr.v();
		System.out.println(cm.getMaxSdkVersion());
		System.out.println(cm.getMinSdkVersion());
		System.out.println(cm.getOutputDir());
		System.out.println(cm.getSdkDBDir());
		System.out.println(cm.getTestSetDir());

		List<SdkAPIs> sdkList = locadSdkAPIs();
		for (int i = 0; i < sdkList.size(); ++i) {
			sdkList.get(i).dump();
		}

		String apkPath = "/home/hedj/Work/android/fdroidNewest/android.game.prboom_31.apk";
		try {
			analysze(apkPath, sdkList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
