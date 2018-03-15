package ict.pag.main;

import java.io.File;

import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConfigMgr;

public class Main {

	/**
	 * @param args
	 *            Program arguments. args[0] = path to apk-file or dir to a group apk-files.
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			printUsage();
			return;
		}
		ConfigMgr cm = ConfigMgr.v();
		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		sdkMgr.dump();

		final long beforeRun = System.nanoTime();
		File file = new File(args[0]);
		if (file.isDirectory()) {
			for (File inFile : file.listFiles()) {
				String name = inFile.getName();
				if (!name.endsWith(".apk")) {
					System.err.println(name + " is not an apk file!");
					continue;
				} else {
					APICompatAnalysis can = new APICompatAnalysis(inFile.getAbsolutePath());
					can.setSdkMgr(sdkMgr);
					can.runAnalysis();
				}
			}
		} else {
			if (!args[0].endsWith(".apk")) {
				System.err.println("args[0] should be an apk file!");
			} else {
				APICompatAnalysis can = new APICompatAnalysis(file.getAbsolutePath());
				can.setSdkMgr(sdkMgr);
				can.runAnalysis();
			}
		}
		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

	private static void printUsage() {
		System.out.println("FlowDroid (c) Program Analysis Group @ ICT, CAS");
		System.out.println();
		System.out.println("\tCorrect arguments: [0] = apk-file or [0] = apk-files-directory");
	}

}
