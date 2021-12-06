package ict.pag.datalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SdkAPIs {
	private final int mApiLevel;
	private final Set<String> mTypeSigs;
	private final Map<String, String> method2decl; // map method to its declaring type
	private final Map<String, String> field2decl; // map field to its declaring type
	private final Map<String, Set<String>> type2apis; // map type to its methods' desc and fields' sig.

	public SdkAPIs(LBWorkspaceConnector conn) {
		this.mApiLevel = conn.getSdkVersion();
		long beforeRun = System.nanoTime();
		Logger logger = LoggerFactory.getLogger(getClass());
		logger.info("start to load SDK " + mApiLevel);
		this.field2decl = FactsLoader.getApplicationFieldToDeclMap(conn);
		this.method2decl = FactsLoader.getApplicationMethodToDeclMap(conn);
		// <signature, descriptor>
		Map<String, String> methodToDesc = FactsLoader.getApplicationMethodToDescMap(conn);
		type2apis = new HashMap<>();
		transferMethods(type2apis, method2decl, methodToDesc);
		transferFields(type2apis, field2decl);
		mTypeSigs = FactsLoader.getApplicationTypes(conn);
		// map type to its father type.
		Map<String, String> type2father = new HashMap<>();
		Map<String, String> tmp = FactsLoader.getDirectSuperClass(conn);
		for (Entry<String, String> entry : tmp.entrySet()) {
			if (mTypeSigs.contains(entry.getKey())) {
				type2father.put(entry.getKey(), entry.getValue());
			}
		}
		logger.info(
				"finish loading SDK" + mApiLevel + ", run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

	private void transferMethods(Map<String, Set<String>> mType2apis, Map<String, String> method2decl,
			Map<String, String> methodToDesc) {
		for (Entry<String, String> entry : method2decl.entrySet()) {
			String mthd = entry.getKey();
			String decl = entry.getValue();
			String desc = methodToDesc.get(mthd);
			if (mType2apis.containsKey(decl)) {
				Set<String> apis = mType2apis.get(decl);
				apis.add(desc);
			} else {
				Set<String> apis = new HashSet<>();
				apis.add(desc);
				mType2apis.put(decl, apis);
			}
		}
	}

	private void transferFields(Map<String, Set<String>> mType2apis, Map<String, String> field2decl) {
		for (Entry<String, String> entry : field2decl.entrySet()) {
			String mthd = entry.getKey();
			String decl = entry.getValue();
			if (mType2apis.containsKey(decl)) {
				Set<String> apis = mType2apis.get(decl);
				apis.add(mthd);
			} else {
				Set<String> apis = new HashSet<>();
				apis.add(mthd);
				mType2apis.put(decl, apis);
			}
		}
	}

	public boolean containMethod(String sig) {
		return this.method2decl.containsKey(sig);
	}

	public boolean containField(String sig) {
		return this.field2decl.containsKey(sig);
	}

	public boolean containAPI(String type, String desc) {
		if (!type2apis.containsKey(type)) {
			return false;
		}
		Set<String> apis = type2apis.get(type);
		return apis.contains(desc);
	}

	public Set<String> getMethodSigs() {
		return method2decl.keySet();
	}

	public Set<String> getFieldSigs() {
		return field2decl.keySet();
	}

	public void dump() {
		System.out.println("API LEVEL: " + this.mApiLevel);
		System.out.println("TYPE SIZE: " + this.mTypeSigs.size());
		System.out.println("METHOD SIZE: " + this.method2decl.size());
		System.out.println("FIELD SIZE: " + this.field2decl.size());
	}
}
