package org.qp.android.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Objects;

public final class DirUtil {
    private static final String TAG = DirUtil.class.getSimpleName();

    /**
     * If there is only one folder in the folder and nothing else,
     * recursively expands the folder until either there is nothing left,
     * or there will be a folder in which there will be something other than one subfolder.
     */
    public static void normalizeGameDirectory(File dir) {
        var it = dir;
        while (true) {
            var files = it.listFiles();
            if (Objects.requireNonNull(files).length != 1 || !files[0].isDirectory()) {
                break;
            }
            it = files[0];
        }
        if (it == dir) {
            return;
        }
        Log.i(TAG,"Normalizing game directory: " + dir.getAbsolutePath());
        for (var file : Objects.requireNonNull(it.listFiles())) {
            var dest = new File(dir.getAbsolutePath(), file.getName());
            Log.d(TAG,"Moving game file"+ file.getAbsolutePath() + " to " + dest.getAbsolutePath());
            if (file.renameTo(dest)) {
                Log.i(TAG,"Renaming file success");
            } else {
                Log.e(TAG,"Renaming file error");
            }
        }
    }

    public static boolean doesDirectoryContainGameFiles(@NonNull File dir) {
        for (var file : Objects.requireNonNull(dir.listFiles())) {
            var name = file.getName();
            var lcName = name.toLowerCase();
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                return true;
            }
        }
        return false;
    }
}
