package ict.pag.datalog;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

	public boolean containAPI(int idx, String sig) {
		// not check level 20;
		if(idx == 20) return true;
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
}
