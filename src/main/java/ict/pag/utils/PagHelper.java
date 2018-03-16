package ict.pag.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import ict.pag.global.ConfigMgr;
import soot.Value;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.internal.AbstractJimpleIntBinopExpr;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JNeExpr;

public class PagHelper {

	public static void listSetElements(Set<?> set) {
		Iterator<?> it = set.iterator();
		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

	public static void showListElements(List<?> list) {
		for (int i = 0; i < list.size(); ++i) {
			System.out.println(list.get(i));
		}
	}

	public static String[] trimArray(String[] args) {
		for (int i = 0; i < args.length; ++i) {
			args[i] = args[i].trim();
		}
		return args;
	}

	public static boolean isConcernIfStmt(IfStmt is, HashSet<Value> vs) {
		Value cond = is.getCondition();
		boolean flag = false;
		flag |= (cond instanceof JGtExpr);
		flag |= (cond instanceof JGeExpr);
		flag |= (cond instanceof JEqExpr);
		flag |= (cond instanceof JNeExpr);
		flag |= (cond instanceof JLeExpr);
		flag |= (cond instanceof JLtExpr);
		if (flag == false) {
			return false;
		} else {
			AbstractJimpleIntBinopExpr jbe = (AbstractJimpleIntBinopExpr) cond;
			Value v1 = jbe.getOp1();
			Value v2 = jbe.getOp2();
			if (vs.contains(v1) && v2 instanceof IntConstant) {
				return true;
			} else if (vs.contains(v2) && v1 instanceof IntConstant) {
				return true;
			}
		}
		return false;
	}

	public static Set<Integer> fetchKillingSet(IfStmt stmt) {
		int mMinVersion = ConfigMgr.v().getMinSdkVersion();
		int mMaxVersion = ConfigMgr.v().getMaxSdkVersion();
		Set<Integer> ret = new HashSet<Integer>();
		AbstractJimpleIntBinopExpr cond = (AbstractJimpleIntBinopExpr) stmt.getCondition();
		Value op1 = cond.getOp1();
		Value op2 = cond.getOp2();
		if (op2 instanceof IntConstant) {
			IntConstant v2 = (IntConstant) op2;
			int value = v2.value;
			if (cond instanceof JGtExpr) {
				int top = Math.min(value, mMaxVersion);
				for (int i = mMinVersion; i <= top; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JGeExpr) {
				int top = Math.min(value, mMaxVersion);
				for (int i = mMinVersion; i < top; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JEqExpr) {
				for (int i = mMinVersion; i <= mMaxVersion; ++i) {
					if (i != value) {
						ret.add(i);
					}
				}
			} else if (cond instanceof JNeExpr) {
				if (value <= mMaxVersion && value >= mMinVersion) {
					ret.add(value);
				}
			} else if (cond instanceof JLeExpr) {
				int bottom = Math.max(mMinVersion, value);
				for (int i = bottom + 1; i <= mMaxVersion; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JLtExpr) {
				int bottom = Math.max(mMinVersion, value);
				for (int i = bottom; i <= mMaxVersion; ++i) {
					ret.add(i);
				}
			}
		} else {
			assert op1 instanceof IntConstant;
			IntConstant v1 = (IntConstant) op1;
			int value = v1.value;
			if (cond instanceof JGtExpr) {
				int bottom = Math.max(mMinVersion, value);
				for (int i = bottom; i <= mMaxVersion; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JGeExpr) {
				int bottom = Math.max(mMinVersion, value);
				for (int i = bottom + 1; i <= mMaxVersion; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JEqExpr) {
				for (int i = mMinVersion; i <= mMaxVersion; ++i) {
					if (i != value) {
						ret.add(i);
					}
				}
			} else if (cond instanceof JNeExpr) {
				if (value <= mMaxVersion && value >= mMinVersion) {
					ret.add(value);
				}
			} else if (cond instanceof JLeExpr) {
				int top = Math.min(value, mMaxVersion);
				for (int i = mMinVersion; i < top; ++i) {
					ret.add(i);
				}
			} else if (cond instanceof JLtExpr) {
				int top = Math.min(value, mMaxVersion);
				for (int i = mMinVersion; i <= top; ++i) {
					ret.add(i);
				}
			}
		}
		return ret;
	}

	public static String descriptor(SootMethod m) {
		StringBuilder builder = new StringBuilder();
		builder.append(m.getReturnType().toString());
		builder.append("(");
		for (int i = 0; i < m.getParameterCount(); i++) {
			builder.append(m.getParameterType(i));

			if (i != m.getParameterCount() - 1) {
				builder.append(",");
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
