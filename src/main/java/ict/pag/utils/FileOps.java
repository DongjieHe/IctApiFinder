package ict.pag.utils;

import java.io.File;

public class FileOps {
	public static File findFileOrThrow(String file, String msg) {
		if (file == null || file.trim().isEmpty()) {
			throw new RuntimeException(msg);
		}
		File f = new File(file);
		if (!f.exists() || !f.isFile() || !f.canRead()) {
			throw new RuntimeException(msg);
		}
		return f;
	}

	public static File findDirOrThrow(String dir, String msg) {
		if (dir == null || dir.trim().isEmpty()) {
			throw new RuntimeException(msg);
		}
		File d = new File(dir);
		if (!d.exists() || !d.isDirectory()) {
			throw new RuntimeException(msg);
		}
		return d;
	}

	public static void main(String args[]) {
		File testFile = findFileOrThrow("/home/hedj/IELTS-Speaking.odt", "not a file");
		System.out.println(testFile.getName() + " == " + testFile.getPath());
		File testFile1 = findDirOrThrow("/home/hedj", "not a directory");
		System.out.println(testFile1.getName() + " == " + testFile1.getPath());
	}
}
