package com.study.util;

import java.io.File;

public class FileUtil {

	public static boolean deleteDir(File dir) {
		if(dir.isDirectory()) {
			String[] children = dir.list();
			//递归删除目录中的子目录
			for(int i=0; i<children.length;i++) {
				boolean res = deleteDir(new File(dir,children[i]));
				if(!res) {
					return false;
				}
			}
		}
		return dir.delete();
	}
}
