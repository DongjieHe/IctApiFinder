package ict.pag.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Value;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.DirectedGraph;

/**
 * !TODO we could construct a ICFG with basicblock as its node.
 * if a method have no ifStmt that we concern, we could use one note in icfg to represent
 * the method.
 *
 * Let this work be done in the future.
 * */
public class FinderInterproceduralCFG implements BiDiInterproceduralCFG<Block, SootMethod> {

	@Override
	public SootMethod getMethodOf(Block n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getSuccsOf(Block n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<SootMethod> getCalleesOfCallAt(Block n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Block> getCallersOf(SootMethod m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Block> getCallsFromWithin(SootMethod m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Block> getStartPointsOf(SootMethod m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Block> getReturnSitesOfCallAt(Block n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCallStmt(Block stmt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isExitStmt(Block stmt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStartPoint(Block stmt) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<Block> allNonCallStartNodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFallThroughSuccessor(Block stmt, Block succ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isBranchTarget(Block stmt, Block succ) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Block> getPredsOf(Block u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Block> getEndPointsOf(SootMethod m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getPredsOfCallAt(Block u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Block> allNonCallEndNodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectedGraph<Block> getOrCreateUnitGraph(SootMethod body) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Value> getParameterRefs(SootMethod m) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isReturnSite(Block n) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReachable(Block u) {
		// TODO Auto-generated method stub
		return false;
	}

}
