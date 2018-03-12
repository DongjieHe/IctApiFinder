package ict.pag.datalog;

import java.util.List;

public class LBWorkspaceConnector implements WorkspaceAPI {
	private int sdkVersion; // an unique string which mark this DB.
	private String workspace;
	private String workDirectory;
	Executor executor;

	public LBWorkspaceConnector(Executor exc, String dir, int sv) {
		sdkVersion = sv;
		executor = exc;
		workDirectory = dir;
	}

	public void connect(String db) {
		workspace = db;
	}

	public void disconnect() {
		workspace = null;
	}

	@Override
	public List<String> query(String qstr) {
		if (workspace == null) {
			throw new RuntimeException("DB not connect");
		}
		String cmd = "bloxbatch -db " + workspace + " -query " + qstr;
		return executor.execute(workDirectory, cmd);
	}

	@Override
	public void echo(String message) {
		throw new UnsupportedOperationException("echo");
	}

	@Override
	public void startTimer() {
		throw new UnsupportedOperationException("startTimer");
	}

	@Override
	public void elapsedTime() {
		throw new UnsupportedOperationException("elapsedTime");
	}

	@Override
	public void transaction() {
		throw new UnsupportedOperationException("transaction");
	}

	@Override
	public void timedTransaction(String message) {
		throw new UnsupportedOperationException("timedTransaction");
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException("commit");
	}

	@Override
	public void createDB(String database) {
		throw new UnsupportedOperationException("createDB");
	}

	@Override
	public void openDB(String database) {
		throw new UnsupportedOperationException("openDB");
	}

	@Override
	public void addBlock(String logiqlString) {
		throw new UnsupportedOperationException("addBlock");
	}

	@Override
	public void addBlockFile(String filePath) {
		throw new UnsupportedOperationException("addBlockFile");
	}

	@Override
	public void addBlockFile(String filePath, String blockName) {
		throw new UnsupportedOperationException("addBlockFile");
	}

	@Override
	public void execute(String logiqlString) {
		throw new UnsupportedOperationException("execute");
	}

	@Override
	public void executeFile(String filePath) {
		throw new UnsupportedOperationException("executeFile");
	}

	public int getSdkVersion() {
		return sdkVersion;
	}

	public static void main(String args[]) {
		Executor executor = new Executor();
		LBWorkspaceConnector lbcon = new LBWorkspaceConnector(executor,
				"/home/hedj/Work/android/generatedFacts/android-7.1.2_r9/android-7.1.2_r9-sdk-db", 25);
		lbcon.connect("database");
		List<String> superclass = lbcon.query("Superclass");
		for (int i = 0; i < superclass.size(); ++i) {
			System.out.println(superclass.get(i));
		}
	}

}
