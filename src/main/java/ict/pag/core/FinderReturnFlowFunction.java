package ict.pag.core;

import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;

public class FinderReturnFlowFunction implements FlowFunction<FinderFact> {

	public FinderReturnFlowFunction() {

	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<>();
		retSet.add(source);
		return retSet;
	}

}
