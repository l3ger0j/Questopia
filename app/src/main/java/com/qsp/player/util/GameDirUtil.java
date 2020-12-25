package com.qsp.player.util;

import java.io.File;

public final class GameDirUtil {

    /**
     * Если в папке есть только одна папка, и больше ничего, рекурсивно разворачивает папку до тех
     * пор, пока или ничего не останется, или останется папка, в которой будет что-то другое, кроме
     * одной вложенной папки.
     */
    public static void normalizeGameDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length != 1 || !files[0].isDirectory()) return;

        for (File file : files[0].listFiles()) {
            file.renameTo(new File(dir.getAbsolutePath(), file.getName()));
        }
        files[0].delete();
        normalizeGameDirectory(dir);
    }

    public static boolean doesDirectoryContainGameFiles(File dir) {
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (name == null) continue;

            String lcName = name.toLowerCase();
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) return true;
        }

        return false;
    }
}
