package ict.pag.main;

import soot.jimple.infoflow.android.SetupApplication;

public class AndroidApplication extends SetupApplication {
	private String appName;

	public AndroidApplication(String androidJar, String apkFileLocation) {
		super(androidJar, apkFileLocation);
		appName = apkFileLocation.substring(apkFileLocation.lastIndexOf("/") + 1);
		assert appName.endsWith(".apk");
	}

	public int getMinSdkVersion() {
		int minLevel = manifest.getMinSdkVersion();
		if (minLevel == -1) {
			minLevel = 1;
		}
		return minLevel;
	}

	public int targetSdkVersion() {
		int tgtLevel = manifest.targetSdkVersion();
		int minLevel = manifest.getMinSdkVersion();
		if (tgtLevel == -1) {
			if (minLevel != -1) {
				tgtLevel = minLevel;
			} else {
				tgtLevel = 1;
			}
		}
		return tgtLevel;
	}

	public String getAppName() {
		return appName;
	}

}
