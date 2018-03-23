package ict.pag.main;

import java.util.List;

import soot.SootMethod;

public class BugUnit {
	private String bugMsg;
	private List<List<SootMethod>> mPossibleCallStack;
	private int bugType;

	public BugUnit(String msg, List<List<SootMethod>> st, int tp) {
		bugMsg = msg;
		mPossibleCallStack = st;
		bugType = tp;
	}

	public int getBugType() {
		return bugType;
	}

	@Override
	public String toString() {
		String retStr = "BUG: " + bugMsg + "\n";
		return retStr;
	}

	public String toString(boolean fullDetail) {
		String retStr = "BUG: " + bugMsg + "\n";
		assert mPossibleCallStack.size() > 0;
		if (fullDetail == false) {
			retStr += "reachable path:\n";
			List<SootMethod> st = mPossibleCallStack.get(0);
			for (int i = 0; i < st.size(); ++i) {
				SootMethod sm = st.get(i);
				retStr += "\t-->" + sm.getSignature() + "\n";
			}
		} else {
			retStr += "all reachable paths:\n";
			for (int j = 0; j < mPossibleCallStack.size(); ++j) {
				retStr += "\treachable path " + (j + 1) + ":\n";
				List<SootMethod> st = mPossibleCallStack.get(j);
				for (int i = 0; i < st.size(); ++i) {
					SootMethod sm = st.get(i);
					retStr += "\t\t-->" + sm.getSignature() + "\n";
				}
			}
		}
		return retStr;
	}
}
