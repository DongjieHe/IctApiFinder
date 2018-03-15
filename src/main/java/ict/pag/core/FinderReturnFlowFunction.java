package ict.pag.core;

import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;
import soot.SootMethod;
import soot.Unit;

public class FinderReturnFlowFunction implements FlowFunction<FinderFact> {

	public FinderReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt, Unit returnSite) {

	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<FinderFact>();
		retSet.add(source);
		return retSet;
	}

}
