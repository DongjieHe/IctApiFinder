package ict.pag.core;

import heros.FlowFunction;
import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderFlowFunctions implements FlowFunctions<Unit, FinderFact, SootMethod> {
	BiDiInterproceduralCFG<Unit, SootMethod> mInterCFG;

	public FinderFlowFunctions(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		mInterCFG = icfg;
	}

	@Override
	public FlowFunction<FinderFact> getNormalFlowFunction(Unit curr, Unit succ) {
		return new FinderNormalFlowFunction(curr, succ, mInterCFG);
	}

	@Override
	public FlowFunction<FinderFact> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
		return new FinderCallFlowFunction(callStmt, destinationMethod, mInterCFG);
	}

	@Override
	public FlowFunction<FinderFact> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt,
			Unit returnSite) {
		return new FinderReturnFlowFunction();
	}

	@Override
	public FlowFunction<FinderFact> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
		return new FinderCallToReturnFlowFunction(callSite, returnSite);
	}

}