package ict.pag.main;

public class StatUnit {
	private long icfg;
	private long preAna;
	private long ifds;
	private long checkComp;
	private long total;
	private int bugNum;

	public StatUnit() {
		this.icfg = 0;
		this.preAna = 0;
		this.ifds = 0;
		this.checkComp = 0;
		this.total = 0;
		this.bugNum = -1;
	}

	public long getIcfg() {
		return icfg;
	}

	public void setIcfg(long icfg) {
		this.icfg = icfg;
	}

	public long getPreAna() {
		return preAna;
	}

	public void setPreAna(long preAna) {
		this.preAna = preAna;
	}

	public long getIfds() {
		return ifds;
	}

	public void setIfds(long ifds) {
		this.ifds = ifds;
	}

	public long getCheckComp() {
		return checkComp;
	}

	public void setCheckComp(long checkComp) {
		this.checkComp = checkComp;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getBugNum() {
		return bugNum;
	}

	public void setBugNum(int bugNum) {
		this.bugNum = bugNum;
	}

	@Override
	public String toString() {
		return icfg + "," + preAna + "," + ifds + "," + checkComp + "," + total + "," + bugNum;
	}

}
