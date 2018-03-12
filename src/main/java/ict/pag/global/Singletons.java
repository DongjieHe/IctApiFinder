package ict.pag.global;

public class Singletons {
	public final class Global {
		private Global() {
		}
	}

	protected Global g = new Global();
	private ConfigMgr instance_ConfigMgr;

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

	protected void release_ConfigMgr() {
		instance_ConfigMgr = null;
	}
}
