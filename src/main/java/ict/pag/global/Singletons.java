package ict.pag.global;

public class Singletons {
	public static final class Global {
		private Global() {
		}
	}

	protected Global g = new Global();
	private volatile ConfigMgr instance_ConfigMgr;

	public ConfigMgr get_ConfigMgr() {
		if (instance_ConfigMgr == null) {
			synchronized (this) {
				if (instance_ConfigMgr == null) {
					instance_ConfigMgr = new ConfigMgr(g);
				}
			}
		}
		return instance_ConfigMgr;
	}

	private volatile ConcernUnits instance_ConcernUnits;

	public ConcernUnits get_ConcernUnits() {
		if (instance_ConcernUnits == null) {
			synchronized (this) {
				if (instance_ConcernUnits == null) {
					instance_ConcernUnits = new ConcernUnits(g);
				}
			}
		}
		return instance_ConcernUnits;
	}

	public void release_ConcernUnits() {
		instance_ConcernUnits = null;
	}
}
