package com.qsp.player.util;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;

public final class GameDirUtil {

    /**
     * Если в папке есть только одна папка, и больше ничего, рекурсивно разворачивает папку до тех
     * пор, пока или ничего не останется, или останется папка, в которой будет что-то другое, кроме
     * одной вложенной папки.
     */
    public static void normalizeGameDirectory(DocumentFile dir) {
        String dirPath = dir.getUri().getPath();
        File dirFile = new File(dirPath);
        File[] subFiles = dirFile.listFiles();
        if (subFiles == null || subFiles.length != 1 || !subFiles[0].isDirectory()) {
            return;
        }
        for (File file : subFiles[0].listFiles()) {
            file.renameTo(new File(dirPath + "/", file.getName()));
        }
        subFiles[0].delete();
        normalizeGameDirectory(dir);
    }

    public static boolean doesDirectoryContainGameFiles(DocumentFile dir) {
        for (DocumentFile file : dir.listFiles()) {
            String name = file.getName();
            if (name == null) continue;

            String lcName = name.toLowerCase();
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) return true;
        }

        return false;
    }
}
