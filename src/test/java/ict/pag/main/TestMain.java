package ict.pag.main;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import ict.pag.global.ConfigMgr;
import soot.Scene;
import soot.SootClass;

public class TestMain {
	public static void main(String[] args) throws IOException {
		PrintStream ps = new PrintStream(new FileOutputStream("MAIN" + new Date().getTime()));
		System.setOut(ps);
		String apkPath = "/home/hedj/Work/android/fdroidNewest/com.alexcruz.papuhwalls_10.apk";
		String androidJarDir = ConfigMgr.v().getSdkDBDir();
		AndroidApplication app = new AndroidApplication(androidJarDir, apkPath);
		app.constructCallgraph();
		System.out.println("application class: ");
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			System.out.println("\t" + sc.getName());
		}
		System.out.println("basic class: ");
		for (String sc : Scene.v().getBasicClasses()) {
			System.out.println("\t" + sc);
		}
		System.out.println("library class: ");
		for (SootClass sc : Scene.v().getLibraryClasses()) {
			System.out.println("\t" + sc.getName());
		}
		System.out.println("Phantom class: ");
		for (SootClass sc : Scene.v().getPhantomClasses()) {
			System.out.println("\t" + sc.getName());
		}
	}
}
