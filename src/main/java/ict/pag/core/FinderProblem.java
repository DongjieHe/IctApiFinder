package ict.pag.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderProblem
		extends DefaultJimpleIFDSTabulationProblem<FinderFact, BiDiInterproceduralCFG<Unit, SootMethod>> {

	protected final Map<Unit, Set<FinderFact>> initialSeeds = new HashMap<>();
	private FinderFact zeroValue = null;
	private int threadNums;

	public FinderProblem(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
		threadNums = -1;
	}

	@Override
	public FinderFact createZeroValue() {
		if (zeroValue == null)
			zeroValue = FinderFact.getZeroAbstraction();
		return zeroValue;
	}

	@Override
	public BiDiInterproceduralCFG<Unit, SootMethod> interproceduralCFG() {
		return super.interproceduralCFG();
	}

	@Override
	public Map<Unit, Set<FinderFact>> initialSeeds() {
		// TODO Auto-generated method stub
		return initialSeeds;
	}

	/**
	 * Adds the given initial seeds to the information flow problem
	 * 
	 * @param unit
	 *            The unit to be considered as a seed
	 * @param seeds
	 *            The abstractions with which to start at the given seed
	 */
	public void addInitialSeeds(Unit unit, Set<FinderFact> seeds) {
		if (this.initialSeeds.containsKey(unit))
			this.initialSeeds.get(unit).addAll(seeds);
		else
			this.initialSeeds.put(unit, new HashSet<>(seeds));
	}

	@Override
	public FlowFunctions<Unit, FinderFact, SootMethod> createFlowFunctionsFactory() {
		return new FinderFlowFunctions(interproceduralCFG());
	}

	public void setThreadNums(int num) {
		this.threadNums = num;
	}

	@Override
	public int numThreads() {
		// return super.numThreads();
		if (threadNums <= 0) {
			return super.numThreads();
		} else {
			return threadNums;
		}
	}

}
