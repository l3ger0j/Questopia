package com.qsp.player.utils;

import android.util.Log;

import java.io.File;
import java.util.Objects;

public final class GameDirUtil {
    private static final String TAG = GameDirUtil.class.getSimpleName();

    /**
     * Если в папке есть только одна папка, и больше ничего, рекурсивно разворачивает папку до тех
     * пор, пока или ничего не останется, или останется папка, в которой будет что-то другое, кроме
     * одной вложенной папки.
     */
    public static void normalizeGameDirectory(File dir) {
        File it = dir;
        while (true) {
            File[] files = it.listFiles();
            if (Objects.requireNonNull(files).length != 1 || !files[0].isDirectory()) {
                break;
            }
            it = files[0];
        }
        if (it == dir) {
            return;
        }
        Log.i(TAG,"Normalizing game directory: " + dir.getAbsolutePath());
        for (File file : Objects.requireNonNull(it.listFiles())) {
            File dest = new File(dir.getAbsolutePath(), file.getName());
            Log.d(TAG,"Moving game file"+ file.getAbsolutePath() + " to " + dest.getAbsolutePath());
            if (file.renameTo(dest)) {
                Log.i(TAG,"Renaming file success");
            } else {
                Log.e(TAG,"Renaming file error");
            }
        }
    }

    public static boolean doesDirectoryContainGameFiles(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            String name = file.getName();
            String lcName = name.toLowerCase();
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                return true;
            }
        }
        return false;
    }
}
