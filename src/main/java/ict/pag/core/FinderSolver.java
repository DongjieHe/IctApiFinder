package ict.pag.core;

import heros.IFDSTabulationProblem;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.solver.fastSolver.IFDSSolver;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderSolver extends IFDSSolver<Unit, FinderFact ,BiDiInterproceduralCFG<Unit, SootMethod>>{

	public FinderSolver(
			IFDSTabulationProblem<Unit, FinderFact, SootMethod, BiDiInterproceduralCFG<Unit, SootMethod>> tabulationProblem) {
		super(tabulationProblem);
	}

}
