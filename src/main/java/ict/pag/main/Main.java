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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ict.pag.datalog.SdkAPIMgr;
import ict.pag.global.ConfigMgr;
import ict.pag.utils.CodeTimer;

class IctAPIAnalysisTask implements Callable<StatUnit> {
	private String mPath;
	private SdkAPIMgr mSdkMgr;
	private boolean mDetail;
	private CodeTimer codeTimer;

	public IctAPIAnalysisTask(String path, SdkAPIMgr sdkMgr, boolean detail) {
		mPath = path;
		mSdkMgr = sdkMgr;
		mDetail = detail;
		codeTimer = new CodeTimer();
	}

	@Override
	public StatUnit call() throws Exception {
		codeTimer.startTimer();
		IctApiFinder can = new IctApiFinder(mPath);
		can.setSdkMgr(mSdkMgr);
		StatUnit su = can.runAnalysis(mDetail);
		can.releaseCallgraph();
		codeTimer.stopTimer();
		su.setTotal(codeTimer.getExecutionTime());
		return su;
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
		final Logger logger = LoggerFactory.getLogger(Main.class);
		CodeTimer codeTimer = new CodeTimer();
		codeTimer.startTimer();
		ConfigMgr cm = ConfigMgr.v();
		SdkAPIMgr sdkMgr = new SdkAPIMgr(cm.getMinSdkVersion(), cm.getMaxSdkVersion(), cm.getSdkDBDir());
		codeTimer.stopTimer();
		logger.info("SDK API loading time " + (codeTimer.getExecutionTime()) / 1E9 + " seconds");
		sdkMgr.dump();

		final String outputFile = "analysis.csv";
		codeTimer.startTimer();
		File file = new File(args[0]);

		if (file.isDirectory()) {
			Set<String> alreadyCalc = new HashSet<String>();
			BufferedReader reader = new BufferedReader(new FileReader(outputFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				String fields[] = line.split(",");
				assert fields.length == 7;
				int time = Integer.parseInt(fields[6]);
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
				if (!name.endsWith(".apk")) {
					logger.error(name + " is not an apk file!");
					continue;
				} else {
					StatUnit runTime = runAnalysis(inFile.getAbsolutePath(), sdkMgr, false);
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
				logger.error("args[0] should be an apk file!");
			} else {
				runAnalysis(file.getAbsolutePath(), sdkMgr, true);
			}
		}

		codeTimer.stopTimer();
		logger.info("Analysis has run for " + (codeTimer.getExecutionTime()) / 1E9 + " seconds");
	}

	private static StatUnit runAnalysis(String path, SdkAPIMgr sdkMgr, boolean detail) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Callable<StatUnit> callable = new IctAPIAnalysisTask(path, sdkMgr, detail);
		FutureTask<StatUnit> future = new FutureTask<StatUnit>(callable);
		executor.execute(future);
		StatUnit result = null;
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
		if (result == null) {
			result = new StatUnit();
		}
		return result;
	}

	private static void printUsage() {
		System.out.println("IctApiFinder (c) Program Analysis Group @ ICT, CAS");
		System.out.println();
		System.out.println("\tCorrect arguments: [0] = apk-file or [0] = apk-files-directory");
	}

}
