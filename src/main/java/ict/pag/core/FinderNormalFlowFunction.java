package ict.pag.core;

import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;
import ict.pag.global.ConcernUnits;
import soot.Unit;
import soot.jimple.internal.JIfStmt;

public class FinderNormalFlowFunction implements FlowFunction<FinderFact> {
	private Unit mCurr;
	private Unit mSucc;

	public FinderNormalFlowFunction(Unit curr, Unit succ) {
		mCurr = curr;
		mSucc = succ;
	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<FinderFact>();
		if (ConcernUnits.v().contains(mCurr)) {
			JIfStmt stmt = (JIfStmt) mCurr;

		} else {
			retSet.add(source);
		}
		return retSet;
	}

}
