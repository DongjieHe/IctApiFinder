package ict.pag.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			printUsage();
			return;
		}
		ConfigMgr cm = ConfigMgr.v();
		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		sdkMgr.dump();

		final String outputFile = "analysis.csv";
		final long beforeRun = System.nanoTime();
		File file = new File(args[0]);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		if (file.isDirectory()) {
			Set<String> alreadyCalc = new HashSet<String>();
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String fields[] = line.split(",");
				assert fields.length == 2;
				int time = Integer.parseInt(fields[1]);
				if (time != -1) {
					alreadyCalc.add(fields[0]);
				}
			}
			reader.close();
			for (File inFile : file.listFiles()) {
				String name = inFile.getName();
				if (alreadyCalc.contains(name)) {
					continue;
				}
				System.gc();
				if (!name.endsWith(".apk")) {
					System.err.println(name + " is not an apk file!");
					continue;
				} else {
					int runTime = runAnalysis(executor, inFile.getAbsolutePath(), sdkMgr, true);
					try {
						FileWriter fout = new FileWriter(outputFile, true);
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
				runAnalysis(executor, file.getAbsolutePath(), sdkMgr, true);
			}
		}
		executor.shutdown();
		System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
	}

	private static Integer runAnalysis(ExecutorService executor, String path, SdkAPIMgr sdkMgr, boolean detail) {
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
		}
		return result;
	}

	private static void printUsage() {
		System.out.println("FlowDroid (c) Program Analysis Group @ ICT, CAS");
		System.out.println();
		System.out.println("\tCorrect arguments: [0] = apk-file or [0] = apk-files-directory");
	}

}
