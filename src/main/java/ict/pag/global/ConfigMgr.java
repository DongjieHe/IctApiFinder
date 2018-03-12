package ict.pag.global;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ConfigMgr {
	private String sdkDBDir = null;
	private int minSdkVersion;
	private int maxSdkVersion;
	private String outputDir = null;
	private String testSetDir = null;

	private void loadFromConfigFile() {
		try {
			JSONObject jsonObj = new JSONObject(new JSONTokener(new FileReader(new File("data/config.json"))));
			sdkDBDir = jsonObj.getString("SDK_DB_DIR").toString();
			outputDir = jsonObj.getString("OUT_DIR").toString();
			testSetDir = jsonObj.getString("APK_TEST_SET").toString();
			minSdkVersion = jsonObj.getInt("MIN_SDK_VERSION");
			maxSdkVersion = jsonObj.getInt("MAX_SDK_VERSION");
		} catch (JSONException | FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	public ConfigMgr(Singletons.Global g) {
		if (g == null) {
			throw new RuntimeException("invalid argument: Singletons.Global g is null!");
		}
		loadFromConfigFile();
	}

	public static ConfigMgr v() {
		return G.v().get_ConfigMgr();
	}

	public String getSdkDBDir() {
		return sdkDBDir;
	}

	public int getMinSdkVersion() {
		return minSdkVersion;
	}

	public int getMaxSdkVersion() {
		return maxSdkVersion;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public String getTestSetDir() {
		return testSetDir;
	}
}
