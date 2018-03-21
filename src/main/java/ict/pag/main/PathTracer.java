package ict.pag.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class PathTracer {
	private BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private SootMethod currMethod;
	private Stack<SootMethod> callStack;
	private Unit mUnit;

	public PathTracer(BiDiInterproceduralCFG<Unit, SootMethod> cfg, Unit unit) {
		icfg = cfg;
		currMethod = icfg.getMethodOf(unit);
		mUnit = unit;
		callStack = null;
	}

	private void traceHelper(Stack<SootMethod> stack, Set<SootMethod> visit, Set<SootMethod> entrySet) {
		if (stack.isEmpty()) {
			return;
		}
		SootMethod top = stack.peek();
		if (entrySet.contains(top)) {
			// find one path
			callStack = new Stack<SootMethod>();
			while (!stack.isEmpty()) {
				callStack.push(stack.pop());
			}
			return;
		}
		visit.add(top);
		Collection<Unit> callerUnits = icfg.getCallersOf(top);
		for (Unit unit : callerUnits) {
			if(!icfg.isReachable(unit)) {
				continue;
			}
			SootMethod caller = icfg.getMethodOf(unit);
			if (visit.contains(caller)) {
				continue;
			}
			stack.push(caller);
			traceHelper(stack, visit, entrySet);
			if (stack.isEmpty()) {
				return;
			}
			stack.pop();
		}
	}

	public void trace() {
		List<SootMethod> entrySm = Scene.v().getEntryPoints();
		Set<SootMethod> entrySet = new HashSet<SootMethod>(entrySm);
		Set<SootMethod> visit = new HashSet<SootMethod>();
		Stack<SootMethod> stack = new Stack<SootMethod>();
		stack.push(currMethod);
		traceHelper(stack, visit, entrySet);
	}

	public Stack<SootMethod> getCallStack() {
		return callStack;
	}
}
