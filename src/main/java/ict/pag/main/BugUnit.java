package ict.pag.main;

import java.util.Stack;

import soot.SootMethod;

public class BugUnit {
	private String bugMsg;
	private Stack<SootMethod> mCallStack;

	public BugUnit(String msg, Stack<SootMethod> st) {
		bugMsg = msg;
		mCallStack = st;
	}

	@Override
	public String toString() {
		String retStr = "BUG: " + bugMsg + "\n";
		retStr += "reachable path:\n";
		while(!mCallStack.isEmpty()) {
			SootMethod sm = mCallStack.pop();
			retStr += "\t-->" + sm.getSignature() + "\n";
		}
		return retStr;
	}

}
