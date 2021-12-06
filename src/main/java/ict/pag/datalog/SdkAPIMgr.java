package ict.pag.datalog;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ict.pag.utils.PagHelper;
import soot.SootClass;
import soot.SootMethod;

public class SdkAPIMgr {
	private final int minLevel;
	private final int maxLevel;
	private final Map<Integer, SdkAPIs> level2SdkAPIs;
	private final Set<String> apiSet;

	public SdkAPIMgr(int mMin, int mMax, String sdkDir) {
		assert mMin <= mMax;
		minLevel = mMin;
		maxLevel = mMax;
		level2SdkAPIs = new HashMap<>();
		apiSet = new HashSet<>();

		Executor executor = new Executor();
		for (int i = mMin; i <= mMax; ++i) {
			if (i == 20) {
				continue; // api level 20 is a special case.
			}
			String sdkPath = sdkDir + File.separator + i;
			LBWorkspaceConnector conn = new LBWorkspaceConnector(executor, sdkPath, i);
			conn.connect("database");
			SdkAPIs tmp = new SdkAPIs(conn);
			level2SdkAPIs.put(i, tmp);
			apiSet.addAll(tmp.getFieldSigs());
			apiSet.addAll(tmp.getMethodSigs());
		}
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

	public void dump() {
		System.out.println("min: " + minLevel + ", max: " + maxLevel);
		System.out.println("potential API: " + apiSet.size());
//		for (Entry<Integer, SdkAPIs> entry : level2SdkAPIs.entrySet()) {
//			entry.getValue().dump();
//		}
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
