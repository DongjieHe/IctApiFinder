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
	private List<SootMethod> mPath;
	private Unit mUnit;

	public PathTracer(BiDiInterproceduralCFG<Unit, SootMethod> cfg, Unit unit) {
		icfg = cfg;
		currMethod = icfg.getMethodOf(unit);
		mUnit = unit;
		mPath = null;
	}

	private void traceHelper(Stack<SootMethod> stack, Set<SootMethod> visit, Set<SootMethod> entrySet) {
		if (stack.isEmpty()) {
			return;
		}
		SootMethod top = stack.pop();
		visit.add(top);
		Collection<Unit> callerUnits = icfg.getCallersOf(top);
		for (Unit unit : callerUnits) {
			SootMethod caller = icfg.getMethodOf(unit);
			if (visit.contains(caller)) {
				continue;
			}
			stack.push(caller);
			if (entrySet.contains(caller)) {
				// find one path
				mPath = new ArrayList<SootMethod>();
				mPath.add(top);
				while (!stack.isEmpty()) {
					mPath.add(stack.pop());
				}
				return;
			}
			traceHelper(stack, visit, entrySet);
			if (stack.isEmpty()) {
				return;
			}
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

	public List<SootMethod> getCallStackPath() {
		return mPath;
	}
}
