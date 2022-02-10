package com.qsp.player.util;

public final class PathUtil {

    public static String normalizeGameFolderName(String name) {
        String result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;

        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    public static String removeFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx != -1 ? filename.substring(0, idx) : filename;
    }

    public static String normalizeDirectoryPath(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(0, idx);
    }

    public static String getFilename(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(idx + 1);
    }

    public static String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? null : filename.substring(idx + 1);
    }

    public static String normalizeAbsolutePath(String path) {
        return path.replaceAll("\\\\", "/");
    }
}
