package ict.pag.datalog;

import java.util.List;

public interface WorkspaceAPI {
	public void echo(String message);

	public void startTimer();

	public void elapsedTime();

	public void transaction();

	public void timedTransaction(String message);

	public void commit();

	public void createDB(String database);

	public void openDB(String database);

	public void addBlock(String logiqlString);

	public void addBlockFile(String filePath);

	public void addBlockFile(String filePath, String blockName);

	public void execute(String logiqlString);

	public void executeFile(String filePath);

	public List<String> query(String qstr);
}
