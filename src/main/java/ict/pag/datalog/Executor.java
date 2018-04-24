package ict.pag.datalog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ict.pag.utils.FileOps;

public class Executor {
	private Map<String, String> environment;

	public Executor(Map<String, String> env) {
		this.environment = env;
	}

	public Executor() {
		this.environment = System.getenv();
	}

	public List<String> execute(String workingDirectory, String cmd) {
		final List<String> retList = new ArrayList<String>();
		ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", cmd);
		// Set bash environment.
		Map<String, String> env = pb.environment();
		if (this.environment == null || this.environment.get("LOGICBLOX_HOME") == null) {
			env.put("PATH", "/usr/local/pa-datalog/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
			env.put("LOGICBLOX_HOME", "/usr/local/pa-datalog");
			env.put("JAVA_HOME", "/usr/local/java/openjdk8");
			env.put("LD_LIBRARY_PATH", "/usr/local/pa-datalog/lib/cpp");
			env.put("LB_PAGER_FORCE_START", "1");
		} else {
			env.putAll(this.environment);
		}

		// Set Working Directory.
		if (workingDirectory != null) {
			File cwd = FileOps.findDirOrThrow(workingDirectory, "Working directory is invalid: " + workingDirectory);
			pb.directory(cwd);
		}

		// Contents of STDOUT and STDERR are all in retList.
		pb.redirectErrorStream(true);
		try {
			Process process = pb.start();
			final InputStream is = process.getInputStream();
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			try {
				executorService.submit(new Runnable() {
					public void run() {
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
						String line = null;
						try {
							while ((line = bufferedReader.readLine()) != null) {
								retList.add(line.trim());
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				executorService.shutdown();
			}
			int returnCode = process.waitFor();
			if (returnCode != 0) {
				throw new RuntimeException("Command exited with non-zero status:\n " + cmd);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return retList;
	}

	public List<String> execute(String cmd) {
		return execute(null, cmd);
	}
}
