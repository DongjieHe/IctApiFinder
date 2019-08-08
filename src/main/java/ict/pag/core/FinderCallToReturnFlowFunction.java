package ict.pag.core;

import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;
import ict.pag.global.ConcernUnits;
import soot.Unit;

public class FinderCallToReturnFlowFunction implements FlowFunction<FinderFact> {
	private Unit mCallSite;

	public FinderCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
		mCallSite = callSite;
	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		if (ConcernUnits.v().containAPI(mCallSite)) {
			ConcernUnits.v().add(mCallSite, source.getLevel());
		}
		Set<FinderFact> retSet = new HashSet<FinderFact>();
		retSet.add(source);
		return retSet;
	}

}
