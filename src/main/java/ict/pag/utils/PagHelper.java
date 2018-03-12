package ict.pag.utils;

import java.io.File;
import java.util.*;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import soot.DexClassProvider;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.options.Options;

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

	// return difference set, ret = src - dst.
	public static <T> Set<T> getDifferenceSet(Set<T> src, Set<T> dst) {
		Set<T> diffSet = new HashSet<T>(src);
		diffSet.removeAll(dst);
		return diffSet;
	}

	public static <T> Set<T> getIntersectionSet(Set<T> src, Set<T> dst) {
		Set<T> intersectionSet = new HashSet<T>(src);
		intersectionSet.retainAll(dst);
		return intersectionSet;
	}

	public static int getMinSdkVersion(String apkPath) throws Exception {
		ProcessManifest manifest;
		manifest = new ProcessManifest(apkPath);
		int minLevel = manifest.getMinSdkVersion();
		if (minLevel == -1) {
			minLevel = 1;
		}
		return minLevel;
	}

	public static int getTargetSdkVersion(String apkPath) throws Exception {
		ProcessManifest manifest;
		int api = 1;
		manifest = new ProcessManifest(apkPath);
		api = manifest.targetSdkVersion();
		int minLevel = manifest.getMinSdkVersion();
		if (api == -1) {
			if (minLevel != -1) {
				api = minLevel;
			} else {
				api = 1;
			}
		}
		return api;
	}

	public static Set<SootClass> loadAPKClasses(String path) throws Exception {
		G.v();
		G.reset();
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_process_multiple_dex(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_process_dir(Collections.singletonList(path));
		Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
		Options.v().set_keep_line_number(false);
		Options.v().set_keep_offset(false);
		Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
		Options.v().set_soot_classpath(
				".:/usr/local/java/jdk8/jre/lib/rt.jar:/usr/local/java/jdk8/jre/lib/jce.jar:/usr/local/java/jdk8/jre/lib/jsse.jar");

		Scene scene = Scene.v();
		Set<SootClass> classes = new HashSet<SootClass>();
		File apk = new File(path);
		DexBackedDexFile dexFile;
		int api = PagHelper.getTargetSdkVersion(path);
		dexFile = DexFileFactory.loadDexFile(apk, Opcodes.forApi(api));
		Set<String> classNames = DexClassProvider.classesOfDex(dexFile);
		for (String className : classNames) {
			SootClass c = scene.loadClass(className, SootClass.BODIES);
			classes.add(c);
		}
		scene.loadNecessaryClasses();
		return classes;
	}
}
