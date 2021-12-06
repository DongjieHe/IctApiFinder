package ict.pag.datalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ict.pag.utils.PagHelper;

public class FactsLoader {
	private static final String SEP = ", ";

	public static Set<String> getApplicationTypes(LBWorkspaceConnector conn) {
		String qstr = "'_(arg) <- ApplicationClass(arg).'";
		List<String> retList = conn.query(qstr);
		return new HashSet<>(retList);
	}

	public static Map<String, String> getDirectSuperClass(LBWorkspaceConnector conn) {
		String qstr = "'_(sub, par) <- DirectSuperclass(sub, par).'";
		List<String> retList = conn.query(qstr);
		Map<String, String> retMap = new HashMap<String, String>();
		for (String subClRelStr : retList) {
			String[] sa = PagHelper.trimArray(subClRelStr.split(SEP));
			retMap.put(sa[0], sa[1]);
		}
		return retMap;
	}

	public static Map<String, String> getApplicationFieldToDeclMap(LBWorkspaceConnector conn) {
		String qstr = "'_(arg, arg1) <- ApplicationClass(arg), Field:DeclaringType(?x, arg), Field:Id(?x,arg1).'";
		List<String> retList = conn.query(qstr);
		Map<String, String> retMap = new HashMap<>();
		for (String fieldString : retList) {
			String[] sa = PagHelper.trimArray(fieldString.split(SEP));
			retMap.put(sa[1], sa[0]);
		}
		return retMap;
	}

	public static Map<String, String> getApplicationMethodToDeclMap(LBWorkspaceConnector conn) {
		String qstr = "'_(arg, arg1) <- ApplicationClass(arg), Method:DeclaringType(?x, arg), Method:Id(?x,arg1).'";
		List<String> retList = conn.query(qstr);
		Map<String, String> retMap = new HashMap<>();
		for (String methodString : retList) {
			String[] sa = PagHelper.trimArray(methodString.split(SEP));
			retMap.put(sa[1], sa[0]);
		}
		return retMap;
	}

	public static Map<String, String> getApplicationMethodToDescMap(LBWorkspaceConnector conn) {
		String qstr = "'_(arg1, arg2) <- ApplicationClass(arg), Method:DeclaringType(?x, arg), Method:Id(?x,arg1), Method:Descriptor(?x, arg2).'";
		List<String> retList = conn.query(qstr);
		Map<String, String> retMap = new HashMap<>();
		for (String methodString : retList) {
			String[] sa = PagHelper.trimArray(methodString.split(SEP));
			retMap.put(sa[0], sa[1]);
		}
		return retMap;
	}

	public static void main(String[] args) {
		Executor executor = new Executor();
		LBWorkspaceConnector conn = new LBWorkspaceConnector(executor,
				"/home/hedj/Work/IctApiFinder/IctApiFinder/data/SDK/4", 4);
		conn.connect("database");
		PagHelper.listSetElements(getApplicationTypes(conn));
	}
}
