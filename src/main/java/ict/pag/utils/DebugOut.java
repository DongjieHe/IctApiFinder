package ict.pag.utils;

public class DebugOut {
	private static boolean DEBUG = false;

	public static void setDebugMode(boolean debug) {
		DEBUG = debug;
	}

	public static boolean getDebugMode() {
		return DEBUG;
	}

	public static void print(String arg0) {
		if (DEBUG) {
			System.out.print(arg0);
		}
	}

	public static void println(String arg0) {
		if (DEBUG) {
			System.out.println(arg0);
		}
	}

}
