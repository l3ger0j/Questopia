package com.qsp.player.util;

public final class PathUtil {

    public static String getFilename(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(idx + 1);
    }

    public static String getExtension(String path) {
        int idx = path.lastIndexOf('.');
        return idx == -1 ? null : path.substring(idx + 1);
    }

    public static String removeExtension(String path) {
        int idx = path.lastIndexOf('.');
        return idx != -1 ? path.substring(0, idx) : path;
    }

    public static String removeTrailingSlash(String path) {
        int idx = path.lastIndexOf('/');
        return idx == -1 ? path : path.substring(0, idx);
    }
}
