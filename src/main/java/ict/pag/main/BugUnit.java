package ict.pag.main;

import java.util.List;

import soot.SootMethod;

public class BugUnit {
	private String bugMsg;
	private List<SootMethod> tracePath;

	public BugUnit(String msg, List<SootMethod> path) {
		bugMsg = msg;
		tracePath = path;
	}

	@Override
	public String toString() {
		String retStr = "BUG: " + bugMsg + "\n";
		retStr += "reachable path:\n";
		for (SootMethod sm : tracePath) {
			retStr += "\t-->" + sm.getSignature() + "\n";
		}
		return retStr;
	}

}
