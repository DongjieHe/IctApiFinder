package ict.pag.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.FlowFunction;
import ict.pag.global.ConcernUnits;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderNormalFlowFunction implements FlowFunction<FinderFact> {
	private final Unit mCurr;
	private final Unit mSucc;
	BiDiInterproceduralCFG<Unit, SootMethod> mInterCFG;

	public FinderNormalFlowFunction(Unit curr, Unit succ, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		mCurr = curr;
		mSucc = succ;
		mInterCFG = icfg;
	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<>();
		if (ConcernUnits.v().containIfStmt(mCurr)) {
			JIfStmt stmt = (JIfStmt) mCurr;
			Unit tgt = stmt.getTarget();
			Set<Integer> killSet = ConcernUnits.v().getKillSet(mCurr);
			// assert succs.size() == 2;
			// !TODO why there are more than 2 successors follow an if statement?
			if (mSucc.equals(tgt)) {
				if (!killSet.contains(source.getLevel())) {
					retSet.add(source);
				}
			} else {
				if (killSet.contains(source.getLevel())) {
					retSet.add(source);
				}
			}
		} else {
			retSet.add(source);
			// collect field API live level.
			if (mCurr instanceof AssignStmt) {
				Value right = ((AssignStmt) mCurr).getRightOp();
				if (right instanceof InstanceFieldRef || right instanceof StaticFieldRef) {
					if (ConcernUnits.v().containAPI(mCurr)) {
						ConcernUnits.v().add(mCurr, source.getLevel());
					}
				}
			}
		}
		return retSet;
	}

}
