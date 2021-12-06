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

	public void setIcfg(long icfg) {
		this.icfg = icfg;
	}

	public void setPreAna(long preAna) {
		this.preAna = preAna;
	}

	public void setIfds(long ifds) {
		this.ifds = ifds;
	}

	public void setCheckComp(long checkComp) {
		this.checkComp = checkComp;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public void setBugNum(int bugNum) {
		this.bugNum = bugNum;
	}

	@Override
	public String toString() {
		return icfg + "," + preAna + "," + ifds + "," + checkComp + "," + total + "," + bugNum;
	}

}
