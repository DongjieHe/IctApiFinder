package ict.pag.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConfigMgr;

class IctAPIAnalysisTask implements Callable<Integer> {
	private String mPath;
	private SdkAPIMgr mSdkMgr;
	private boolean mDetail;

	public IctAPIAnalysisTask(String path, SdkAPIMgr sdkMgr, boolean detail) {
		mPath = path;
		mSdkMgr = sdkMgr;
		mDetail = detail;
	}

	@Override
	public Integer call() throws Exception {
		final long beforeRun = System.nanoTime();
		APICompatAnalysis can = new APICompatAnalysis(mPath);
		can.setSdkMgr(mSdkMgr);
		can.runAnalysis(mDetail);
		can.releaseCallgraph();
		final long afterRun = System.nanoTime();
		Integer ans = (int) ((afterRun - beforeRun) / 1E9);
		return ans;
	}

}

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
				System.gc();
				if (!name.endsWith(".apk")) {
					System.err.println(name + " is not an apk file!");
					continue;
				} else {
					int runTime = runAnalysis(file.getAbsolutePath(), sdkMgr, true);
					try {
						FileWriter fout = new FileWriter("analysis.csv", true);
						fout.write(name + "," + runTime + "\n");
						fout.flush();
						fout.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.gc();
			}
		} else {
			if (!args[0].endsWith(".apk")) {
				System.err.println("args[0] should be an apk file!");
			} else {
				runAnalysis(file.getAbsolutePath(), sdkMgr, true);
			}
		}
		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

	private static Integer runAnalysis(String path, SdkAPIMgr sdkMgr, boolean detail) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Callable<Integer> callable = new IctAPIAnalysisTask(path, sdkMgr, detail);
		FutureTask<Integer> future = new FutureTask<Integer>(callable);
		executor.execute(future);
		Integer result = -1;
		try {
			result = future.get(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			future.cancel(true);
		} catch (ExecutionException e) {
			future.cancel(true);
		} catch (TimeoutException e) {
			future.cancel(true);
		} finally {
			executor.shutdown();
		}
		return result;
	}

	private static void printUsage() {
		System.out.println("FlowDroid (c) Program Analysis Group @ ICT, CAS");
		System.out.println();
		System.out.println("\tCorrect arguments: [0] = apk-file or [0] = apk-files-directory");
	}

}
