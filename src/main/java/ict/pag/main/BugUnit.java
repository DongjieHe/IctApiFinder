package ict.pag.main;

import java.util.List;

import soot.SootMethod;

public class BugUnit {
	private final String bugMsg;
	private final List<List<SootMethod>> mPossibleCallStack;
	private final int bugType;

	public BugUnit(String msg, List<List<SootMethod>> st, int tp) {
		bugMsg = msg;
		mPossibleCallStack = st;
		bugType = tp;
	}

	@Override
	public String toString() {
		String warnningOrBug = bugType == 0 ? "WARNING: " : "BUG: ";
		return warnningOrBug + bugMsg + "\n";
	}

	public String toString(boolean fullDetail) {
		if(bugType == 0) {
			return "WARNING: " + bugMsg + "\n";
		}
		StringBuilder retStr = new StringBuilder("BUG: " + bugMsg + "\n");
		assert mPossibleCallStack.size() > 0;
		if (!fullDetail) {
			retStr.append("reachable path:\n");
			List<SootMethod> st = mPossibleCallStack.get(0);
			for (SootMethod sm : st) {
				retStr.append("\t-->").append(sm.getSignature()).append("\n");
			}
		} else {
			retStr.append("all reachable paths:\n");
			for (int j = 0; j < mPossibleCallStack.size(); ++j) {
				retStr.append("\treachable path ").append(j + 1).append(":\n");
				List<SootMethod> st = mPossibleCallStack.get(j);
				for (SootMethod sm : st) {
					retStr.append("\t\t-->").append(sm.getSignature()).append("\n");
				}
			}
		}
		return retStr.toString();
	}
}
