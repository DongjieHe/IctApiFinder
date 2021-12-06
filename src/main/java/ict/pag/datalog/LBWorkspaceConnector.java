package ict.pag.datalog;

import java.util.List;

public class LBWorkspaceConnector implements WorkspaceAPI {
	private final int sdkVersion; // an unique string which mark this DB.
	private String workspace;
	private final String workDirectory;
	Executor executor;

	public LBWorkspaceConnector(Executor exc, String dir, int sv) {
		sdkVersion = sv;
		executor = exc;
		workDirectory = dir;
	}

	public void connect(String db) {
		workspace = db;
	}

	@Override
	public List<String> query(String qstr) {
		if (workspace == null) {
			throw new RuntimeException("DB not connect");
		}
		String cmd = "bloxbatch -db " + workspace + " -query " + qstr;
		return executor.execute(workDirectory, cmd);
	}

	public int getSdkVersion() {
		return sdkVersion;
	}

	public static void main(String[] args) {
		Executor executor = new Executor();
		LBWorkspaceConnector lbcon = new LBWorkspaceConnector(executor, "/home/hedj/Work/IctApiFinder/IctApiFinder/data/SDK/7", 7);
		lbcon.connect("database");
		List<String> superclass = lbcon.query("Superclass");
		for (String s : superclass) {
			System.out.println(s);
		}
	}

}
