package ict.pag.main;

import java.util.HashSet;

import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class SdkIntMustAliasAnalysis extends ForwardFlowAnalysis<Unit, HashSet<Value>> {

	private final String target = "<android.os.Build$VERSION: int SDK_INT>";

	public SdkIntMustAliasAnalysis(DirectedGraph<Unit> graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(HashSet<Value> in, Unit d, HashSet<Value> out) {
		Stmt stmt = (Stmt) d;
		copy(in, out);
		if (stmt instanceof AssignStmt) {
			Value left = ((AssignStmt) stmt).getLeftOp();
			Value right = ((AssignStmt) stmt).getRightOp();
			if (in.contains(right)) {
				out.add(left);
			} else if (right instanceof StaticFieldRef) {
				StaticFieldRef ref = (StaticFieldRef) right;
				String fieldSig = ref.getField().getSignature();
				if (fieldSig.equals(target)) {
					out.add(right);
					out.add(left);
				} else if (in.contains(left)) {
					out.remove(left);
				}
			} else if (in.contains(left)) {
				out.remove(left);
			}
		}
	}

	@Override
	protected HashSet<Value> newInitialFlow() {
		return new HashSet<Value>();
	}

	@Override
	protected void merge(HashSet<Value> in1, HashSet<Value> in2, HashSet<Value> out) {
		out.clear();
		out.addAll(in1);
		out.addAll(in2);
	}

	@Override
	protected void copy(HashSet<Value> source, HashSet<Value> dest) {
		dest.clear();
		dest.addAll(source);
	}
}
