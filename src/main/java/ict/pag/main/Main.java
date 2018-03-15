package ict.pag.main;

import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConfigMgr;

public class Main {

	public static void main(String[] args) {
		String apkPath = "/home/hedj/app-debug.apk";
		ConfigMgr cm = ConfigMgr.v();
		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		sdkMgr.dump();

		final long beforeRun = System.nanoTime();
		APICompatAnalysis can = new APICompatAnalysis(apkPath);
		can.setSdkMgr(sdkMgr);
		can.runAnalysis();
		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

}
