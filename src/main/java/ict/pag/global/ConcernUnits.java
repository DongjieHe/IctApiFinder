package ict.pag.global;

import java.util.HashSet;
import java.util.Set;

import soot.Unit;

public class ConcernUnits {

	Set<Unit> units;

	public ConcernUnits(Singletons.Global g) {
		if (g == null) {
			throw new RuntimeException("invalid argument: Singletons.Global g is null!");
		}
		units = new HashSet<Unit>();
	}

	public static ConcernUnits v() {
		return G.v().get_ConcernUnits();
	}

	public Set<Unit> getUnits() {
		return units;
	}

	public void setUnits(Set<Unit> units) {
		this.units = units;
	}

	public boolean contains(Unit u) {
		return this.units.contains(u);
	}
}
