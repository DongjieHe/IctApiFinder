package ict.pag.global;

public class G extends Singletons {

	private G() {
	}

	public static interface GlobalObjectGetter {
		public G getG();

		public void reset();
	}

	public static G v() {
		return objectGetter.getG();
	}

	public static void reset() {
		objectGetter.reset();
	}

	private static GlobalObjectGetter objectGetter = new GlobalObjectGetter() {

		private G instance = new G();

		@Override
		public G getG() {
			return instance;
		}

		@Override
		public void reset() {
			instance = new G();
		}
	};

	public static void setGlobalObjectGetter(GlobalObjectGetter newGetter) {
		objectGetter = newGetter;
	}
}
