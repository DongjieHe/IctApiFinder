package ict.pag.main;

import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.BaseProcessManifest;

public class AndroidApplication extends SetupApplication {

	private final String apkFileLocation;

	public int getMinSdkVersion() {
		int minLevel = ((BaseProcessManifest<?, ?, ?, ?>) manifest).getMinSdkVersion();
		if (minLevel == -1) {
			minLevel = 1;
		}
		return minLevel;
	}

	public AndroidApplication(String androidJar, String apkFileLocation) {
		super(androidJar, apkFileLocation);
		this.apkFileLocation = apkFileLocation;
	}

	public String getAppName() {
		return apkFileLocation.substring(apkFileLocation.lastIndexOf("/") + 1);
	}
}
