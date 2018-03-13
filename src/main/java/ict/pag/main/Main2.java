package ict.pag.main;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ict.pag.utils.PagHelper;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;

public class Main2 {

	public static void main(String[] args) throws Exception {
		String apkPath = "/home/hedj/app-debug.apk";
		Set<Unit> ifStmtSet = new HashSet<Unit>();
		Set<SootClass> apkClasses = PagHelper.loadAPKClasses(apkPath);
		for (Iterator<SootClass> it = apkClasses.iterator(); it.hasNext();) {
			SootClass sc = it.next();
			System.out.println(sc.getName() + "++++++++++++++++++++++");
			List<SootMethod> sms = sc.getMethods();
			for (SootMethod sm : sms) {
				if (!sm.isConcrete()) {
					continue;
				}
				sm.retrieveActiveBody();
				Body body = sm.getActiveBody();
				System.out.println(body);
				BriefUnitGraph noBug = new BriefUnitGraph(body);
				SdkIntMustAliasAnalysis simaa = new SdkIntMustAliasAnalysis(noBug);
				for (Unit u : body.getUnits()) {
					Stmt stmt = (Stmt) u;
					if (stmt instanceof IfStmt) {
						System.out.println(stmt.toString() + " ; " + stmt.getClass() + "^^^^^^^^^");
						HashSet<Value> flowBefore = simaa.getFlowBefore(u);
						IfStmt is = (IfStmt) stmt;
						if (PagHelper.isConcernIfStmt(is, flowBefore)) {
							ifStmtSet.add(u);
							System.out.println(u);
						}
					}
				} // for unit
			} // for method
		} // for class
		System.out.println(ifStmtSet.size());
	}

}
