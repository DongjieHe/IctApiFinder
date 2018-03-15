package ict.pag.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import heros.FlowFunctions;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class FinderProblem
		extends DefaultJimpleIFDSTabulationProblem<FinderFact, BiDiInterproceduralCFG<Unit, SootMethod>> {

	protected final Map<Unit, Set<FinderFact>> initialSeeds = new HashMap<Unit, Set<FinderFact>>();
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private FinderFact zeroValue = null;

	public FinderProblem(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
		super(icfg);
	}

	/**
	 * Gets whether the given method is an entry point, i.e. one of the initial seeds belongs to the given method
	 * 
	 * @param sm
	 *            The method to check
	 * @return True if the given method is an entry point, otherwise false
	 */
	protected boolean isInitialMethod(SootMethod sm) {
		for (Unit u : this.initialSeeds.keySet())
			if (interproceduralCFG().getMethodOf(u) == sm)
				return true;
		return false;
	}

	@Override
	public FinderFact createZeroValue() {
		if (zeroValue == null)
			zeroValue = FinderFact.getZeroAbstraction();
		return zeroValue;
	}

	protected FinderFact getZeroValue() {
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
			this.initialSeeds.put(unit, new HashSet<FinderFact>(seeds));
	}

	/**
	 * Gets whether this information flow problem has initial seeds
	 * 
	 * @return True if this information flow problem has initial seeds, otherwise false
	 */
	public boolean hasInitialSeeds() {
		return !this.initialSeeds.isEmpty();
	}

	/**
	 * Gets the initial seeds with which this information flow problem has been configured
	 * 
	 * @return The initial seeds with which this information flow problem has been configured.
	 */
	public Map<Unit, Set<FinderFact>> getInitialSeeds() {
		return this.initialSeeds;
	}

	@Override
	public FlowFunctions<Unit, FinderFact, SootMethod> createFlowFunctionsFactory() {
		return new FinderFlowFunctions(interproceduralCFG());
	}

	@Override
	public int numThreads() {
		// return super.numThreads();
		return 1;
	}

	public void setThreadsNum(int n) {
		n = Math.max(1, n);

	}
}
