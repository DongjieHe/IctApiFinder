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
	private final BiDiInterproceduralCFG<Unit, SootMethod> icfg;
	private final SootMethod currMethod;
	private final List<List<SootMethod>> possibleCallStack;

	public PathTracer(BiDiInterproceduralCFG<Unit, SootMethod> cfg, Unit unit) {
		icfg = cfg;
		currMethod = icfg.getMethodOf(unit);
		possibleCallStack = new ArrayList<>();
	}

	private void traceHelper(Stack<SootMethod> stack, Set<SootMethod> visit, Set<SootMethod> entrySet) {
		if (stack.isEmpty()) {
			return;
		}
		if (possibleCallStack.size() > 10) {
			return;
		}
		SootMethod top = stack.peek();
		if (entrySet.contains(top)) {
			// find one path
			Stack<SootMethod> callStack = new Stack<>();
			List<SootMethod> callStack2 = new ArrayList<>();
			while (!stack.isEmpty()) {
				SootMethod sm = stack.pop();
				callStack.push(sm);
				callStack2.add(sm);
			}

			while (!callStack.isEmpty()) {
				SootMethod sm = callStack.pop();
				stack.push(sm);
			}
			possibleCallStack.add(callStack2);
			return;
		}
		visit.add(top);
		Collection<Unit> callerUnits = icfg.getCallersOf(top);
		for (Unit unit : callerUnits) {
			if (!icfg.isReachable(unit)) {
				continue;
			}
			SootMethod caller = icfg.getMethodOf(unit);
			if (visit.contains(caller)) {
				continue;
			}
			stack.push(caller);
			traceHelper(stack, visit, entrySet);
			stack.pop();
			if (possibleCallStack.size() > 10) {
				return;
			}
		}
		visit.remove(top);
	}

	public void trace() {
		List<SootMethod> entrySm = Scene.v().getEntryPoints();
		Set<SootMethod> entrySet = new HashSet<>(entrySm);
		Set<SootMethod> visit = new HashSet<>();
		Stack<SootMethod> stack = new Stack<>();
		stack.push(currMethod);
		traceHelper(stack, visit, entrySet);
	}

	public List<List<SootMethod>> getAllPossibleCallStack() {
		return possibleCallStack;
	}
}
