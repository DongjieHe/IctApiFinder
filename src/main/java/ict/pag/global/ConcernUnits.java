package ict.pag.global;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.Unit;

public class ConcernUnits {

	Map<Unit, Set<Integer>> unit2kill;
	Map<Unit, Set<Integer>> api2live;
	Set<Unit> apis;

	public ConcernUnits(Singletons.Global g) {
		if (g == null) {
			throw new RuntimeException("invalid argument: Singletons.Global g is null!");
		}
		unit2kill = null;
		apis = null;
		api2live = new HashMap<Unit, Set<Integer>>();
	}

	public static ConcernUnits v() {
		return G.v().get_ConcernUnits();
	}

	public Map<Unit, Set<Integer>> getUnitToKillMap() {
		return unit2kill;
	}

	public void setUnitToKillMap(Map<Unit, Set<Integer>> uk) {
		this.unit2kill = uk;
	}

	public boolean containIfStmt(Unit u) {
		return this.unit2kill.containsKey(u);
	}

	public Set<Integer> getKillSet(Unit u) {
		return unit2kill.get(u);
	}

	public Set<Unit> getApiSet() {
		return apis;
	}

	public void setApiSet(Set<Unit> apis) {
		this.apis = apis;
	}

	public boolean containAPI(Unit u) {
		return this.apis.contains(u);
	}

	public void add(Unit unit, Integer level) {
		if (this.api2live.containsKey(unit)) {
			this.api2live.get(unit).add(level);
		} else {
			Set<Integer> liveLevels = new HashSet<Integer>();
			liveLevels.add(level);
			this.api2live.put(unit, liveLevels);
		}
	}

	public Map<Unit, Set<Integer>> getApi2live() {
		return api2live;
	}

}
