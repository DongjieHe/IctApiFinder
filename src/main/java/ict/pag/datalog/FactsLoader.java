package ict.pag.datalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ict.pag.datalog.LBWorkspaceConnector;
import ict.pag.utils.PagHelper;

public class FactsLoader {

	public static Set<String> getApplicationTypes(LBWorkspaceConnector conn) {
		String qstr = "'_(arg) <- ApplicationClass(arg).'";
		List<String> retList = conn.query(qstr);
		return new HashSet<String>(retList);
	}

	public static Set<String> getApplicationMethod(LBWorkspaceConnector conn) {
		String qstr = "'_(args) <- ApplicationMethod(args).'";
		return new HashSet<String>(conn.query(qstr));
	}

	public static Set<String> getApplicationFields(LBWorkspaceConnector conn) {
		String qstr = "'_(arg2) <- ApplicationClass(arg), Field:DeclaringType(?x, arg), Field:Id(?x,arg2).'";
		return new HashSet<String>(conn.query(qstr));
	}

	public static void main(String args[]) {
		Executor executor = new Executor();
		LBWorkspaceConnector conn = new LBWorkspaceConnector(executor,
				"/home/hedj/Work/android/formatResult/android-7.0.0_r1/generatedFacts/android-7.0.0_r1-sdk-db", 24);
		conn.connect("database");
		// PagHelper.listSetElements(getApplicationTypes(conn));
		// PagHelper.listSetElements(getApplicationMethod(conn));
		PagHelper.listSetElements(getApplicationFields(conn));
	}
}
