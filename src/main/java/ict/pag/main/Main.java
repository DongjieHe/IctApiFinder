package ict.pag.main;

import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConfigMgr;
import ict.pag.utils.PagHelper;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;

public class Main {
	private static InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

	public static void main(String[] args) {
		String apkPath = "/home/hedj/hello.apk";
		ConfigMgr cm = ConfigMgr.v();
		String androidJarDir = cm.getSdkDBDir();
		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		sdkMgr.dump();

		final long beforeRun = System.nanoTime();
		final SetupApplication app = new SetupApplication(androidJarDir, apkPath);
		// Set configuration object
		app.setConfig(config);
		System.out.println("Running data flow analysis...");
		app.constructCallgraph();
		app.printEntrypoints();

		try {
			System.out.println("minSDKVersion: " + PagHelper.getMinSdkVersion(apkPath));
			System.out.println("targetSDKVersion: " + PagHelper.getTargetSdkVersion(apkPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

}
