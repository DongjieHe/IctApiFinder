package ict.pag.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import heros.FlowFunction;
import ict.pag.global.ConcernUnits;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.JIfStmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderNormalFlowFunction implements FlowFunction<FinderFact> {
	private Unit mCurr;
	private Unit mSucc;
	BiDiInterproceduralCFG<Unit, SootMethod> mInterCFG;

	public FinderNormalFlowFunction(Unit curr, Unit succ, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		mCurr = curr;
		mSucc = succ;
		mInterCFG = icfg;
	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<FinderFact>();
		if (ConcernUnits.v().containIfStmt(mCurr)) {
			JIfStmt stmt = (JIfStmt) mCurr;
			Unit tgt = stmt.getTarget();
			Set<Integer> killSet = ConcernUnits.v().getKillSet(mCurr);
			List<Unit> succs = mInterCFG.getSuccsOf(mCurr);
			assert succs.size() == 2;
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
		}
		return retSet;
	}

}
