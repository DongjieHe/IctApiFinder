package ict.pag.datalog;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ict.pag.utils.PagHelper;
import soot.SootClass;
import soot.SootMethod;

public class SdkAPIMgr {
	private int minLevel;
	private int maxLevel;
	private Map<Integer, SdkAPIs> level2SdkAPIs;
	private Set<String> apiSet;

	public SdkAPIMgr(int mMin, int mMax, String sdkDir) {
		assert mMin <= mMax;
		minLevel = mMin;
		maxLevel = mMax;
		level2SdkAPIs = new HashMap<Integer, SdkAPIs>();
		apiSet = new HashSet<String>();

		Executor executor = new Executor();
		for (int i = mMin; i <= mMax; ++i) {
			if (i == 20) {
				continue; // api level 20 is a special case.
			}
			String sdkPath = sdkDir + File.separator + "android-" + i;
			LBWorkspaceConnector conn = new LBWorkspaceConnector(executor, sdkPath, i);
			conn.connect("database");
			SdkAPIs tmp = new SdkAPIs(conn);
			level2SdkAPIs.put(i, tmp);
			apiSet.addAll(tmp.getFieldSigs());
			apiSet.addAll(tmp.getMethodSigs());
		}
	}

	public int getMinLevel() {
		return minLevel;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public boolean containAPI(String sig) {
		return apiSet.contains(sig);
	}

	public boolean containAPI(SootMethod sm) {
		SootClass decl = sm.getDeclaringClass();
		if (decl.isApplicationClass()) {
			return false;
		}
		String sig = sm.getSignature();
		if (apiSet.contains(sig)) {
			return true;
		}

		while (decl.hasSuperclass()) {
			decl = decl.getSuperclass();
			sig = SootMethod.getSignature(decl, sm.getName(), sm.getParameterTypes(), sm.getReturnType());
			if (apiSet.contains(sig)) {
				return true;
			}
		}
		return false;
	}

	public boolean containAPI(int idx, String sig) {
		// not check level 20;
		if (idx == 20)
			return true;
		if (level2SdkAPIs.containsKey(idx)) {
			SdkAPIs sdkAPIs = level2SdkAPIs.get(idx);
			return sdkAPIs.containMethod(sig) || sdkAPIs.containField(sig);
		}
		return false;
	}

	public void dump() {
		System.out.println("min: " + minLevel + ", max: " + maxLevel);
		System.out.println("potential API: " + apiSet.size());
		for (Entry<Integer, SdkAPIs> entry : level2SdkAPIs.entrySet()) {
			entry.getValue().dump();
		}
	}

	public boolean containMethodAPI(int level, SootMethod callee) {
		if (level == 20)
			return true;
		if (level2SdkAPIs.containsKey(level)) {
			SdkAPIs sdkAPIs = level2SdkAPIs.get(level);
			String calleeSig = callee.getSignature();
			if (sdkAPIs.containMethod(calleeSig)) {
				return true;
			} else {
				SootClass decl = callee.getDeclaringClass();
				decl.isInterface();
				String desc = PagHelper.descriptor(callee);
				if (decl.isInterface()) {
					// !TODO fix me, I just do for one layer.
					for (SootClass sc : decl.getInterfaces()) {
						if (sdkAPIs.containAPI(sc.getName(), desc)) {
							return true;
						}
					}
				} else {
					while (decl.hasSuperclass()) {
						decl = decl.getSuperclass();
						if (sdkAPIs.containAPI(decl.getName(), desc)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean containFieldAPI(int level, String fieldSig) {
		if (level == 20)
			return true;
		if (level2SdkAPIs.containsKey(level)) {
			SdkAPIs sdkAPIs = level2SdkAPIs.get(level);
			return sdkAPIs.containField(fieldSig);
		}
		return false;
	}
}
