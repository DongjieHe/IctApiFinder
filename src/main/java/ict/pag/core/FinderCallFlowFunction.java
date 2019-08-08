package ict.pag.core;

import java.util.HashSet;
import java.util.Set;

import heros.FlowFunction;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderCallFlowFunction implements FlowFunction<FinderFact> {

	public FinderCallFlowFunction(Unit callStmt, SootMethod destinationMethod,
			BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
	}

	@Override
	public Set<FinderFact> computeTargets(FinderFact source) {
		Set<FinderFact> retSet = new HashSet<FinderFact>();
		retSet.add(source);
		return retSet;
	}

}
