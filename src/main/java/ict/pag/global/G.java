package ict.pag.global;

public class G extends Singletons {

	private G() {
	}

	public interface GlobalObjectGetter {
		G getG();

	}

	public static G v() {
		return objectGetter.getG();
	}

	private static final GlobalObjectGetter objectGetter = new GlobalObjectGetter() {

		private final G instance = new G();

		@Override
		public G getG() {
			return instance;
		}

	};

}
