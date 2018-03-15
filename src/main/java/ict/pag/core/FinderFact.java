package ict.pag.core;

import soot.Unit;
import soot.jimple.infoflow.solver.fastSolver.FastSolverLinkedNode;

public class FinderFact implements FastSolverLinkedNode<FinderFact, Unit> {
	private int level;
	private static FinderFact zeroValue = new FinderFact(-1);

	public FinderFact(int lv) {
		level = lv;
	}

	public static FinderFact getZeroAbstraction() {
		return zeroValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FinderFact other = (FinderFact) obj;
		if (level != other.level)
			return false;
		return true;
	}

	@Override
	public FinderFact clone() {
		FinderFact abs = new FinderFact(this.level);
		return abs;
	}

	@Override
	public void addNeighbor(FinderFact originalAbstraction) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCallingContext(FinderFact callingContext) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPredecessor(FinderFact predecessor) {
		// TODO Auto-generated method stub

	}

	@Override
	public FinderFact getPredecessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FinderFact getActiveCopy() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getLevel() {
		return level;
	}
}
