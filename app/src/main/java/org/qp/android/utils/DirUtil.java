package org.qp.android.utils;

import android.util.Log;

import java.io.File;

public final class DirUtil {
    private static final String TAG = DirUtil.class.getSimpleName();

    /**
     * If there is only one folder in the folder and nothing else,
     * recursively expands the folder until either there is nothing left,
     * or there will be a folder in which there will be something other than one subfolder.
     */
    @SuppressWarnings("ConstantConditions")
    public static void normalizeGameDirectory(File dir) {
        var it = dir;
        if (it == null) return;
        if (it.listFiles() == null) return;

        while (true) {
            var files = it.listFiles();
            if (files.length != 1 || !files[0].isDirectory()) break;
            it = files[0];
        }
        if (it == dir) return;
        Log.i(TAG,"Normalizing game directory: " + dir.getAbsolutePath());

        for (var file : it.listFiles()) {
            var dest = new File(dir.getAbsolutePath(), file.getName());
            Log.d(TAG,"Moving game file"+ file.getAbsolutePath() + " to " + dest.getAbsolutePath());
            if (file.renameTo(dest)) {
                Log.i(TAG,"Renaming file success");
            } else {
                Log.e(TAG,"Renaming file error");
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean doesDirectoryContainGameFiles(File dir) {
        if (dir.listFiles() == null) {
            return false;
        } else {
            for (var file : dir.listFiles()) {
                var name = file.getName();
                var lcName = name.toLowerCase();
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam"))
                    return true;
            }
        }
        return false;
    }
}
