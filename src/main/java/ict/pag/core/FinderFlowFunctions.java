package ict.pag.core;

import heros.FlowFunction;
import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;

public class FinderFlowFunctions implements FlowFunctions<Unit, FinderFact, SootMethod> {

	@Override
	public FlowFunction<FinderFact> getNormalFlowFunction(Unit curr, Unit succ) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowFunction<FinderFact> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowFunction<FinderFact> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod, Unit exitStmt,
			Unit returnSite) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowFunction<FinderFact> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
		// TODO Auto-generated method stub
		return null;
	}

}