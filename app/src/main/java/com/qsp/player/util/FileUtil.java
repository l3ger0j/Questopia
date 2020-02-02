package com.qsp.player.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public final class FileUtil {

    public static final String MIME_TYPE_BINARY = "application/octet-stream";
    public static final String GAME_INFO_FILENAME = "gamestockInfo";

    private static final String TAG = FileUtil.class.getName();

    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String result = path;
        if (result.startsWith("./")) {
            result = result.substring(2);
        }

        return result.replace("\\", "/");
    }

    public static String normalizeGameFolderName(String name) {
        String result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;

        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    public static String removeFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx != -1 ? filename.substring(0, idx) : filename;
    }

    public static DocumentFile getFile(Context context, String uri) {
        if (uri.startsWith("file:///")) {
            return DocumentFile.fromFile(new File(URI.create(uri)));
        }

        return DocumentFile.fromSingleUri(context, Uri.parse(uri));
    }

    public static DocumentFile getDirectory(Context context, String uri) {
        if (uri.startsWith("file:///")) {
            return DocumentFile.fromFile(new File(URI.create(uri)));
        }

        return DocumentFile.fromTreeUri(context, Uri.parse(uri));
    }

    public static DocumentFile findFileByPath(DocumentFile parentDir, String path) {
        int idx = path.indexOf("/");
        if (idx == -1) {
            return findFileIgnoreCase(parentDir, path);
        }

        String dirName = path.substring(0, idx);
        DocumentFile dir = findFileIgnoreCase(parentDir, dirName);
        if (dir == null) {
            return null;
        }

        return findFileByPath(dir, path.substring(idx + 1));
    }

    private static DocumentFile findFileIgnoreCase(DocumentFile parentDir, String displayName) {
        for (DocumentFile f : parentDir.listFiles()) {
            if (f.getName().toLowerCase().equals(displayName.toLowerCase())) {
                return f;
            }
        }

        return null;
    }

    public static String readFileAsString(Context context, DocumentFile file) {
        StringBuilder result = new StringBuilder();
        try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
            InputStreamReader inReader = new InputStreamReader(in);
            try (BufferedReader bufReader = new BufferedReader(inReader)) {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    result.append(line);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file", e);
            return null;
        }

        return result.toString();
    }

    public static void deleteDirectory(DocumentFile dir) {
        for (DocumentFile child : dir.listFiles()) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            } else {
                child.delete();
            }
        }
        dir.delete();
    }

    static String normalizeDirectoryPath(String dirPath) {
        int idx = dirPath.lastIndexOf('/');
        return idx == -1 ? dirPath : dirPath.substring(0, idx);
    }

    static String getFilename(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) {
            return path;
        }

        return path.substring(idx + 1);
    }

    static DocumentFile getParentDirectory(DocumentFile parentDir, String path) {
        int idx = path.indexOf('/');
        if (idx == -1) {
            return parentDir;
        }

        String dirName = path.substring(0, idx);
        DocumentFile dir = parentDir.findFile(dirName);
        if (dir == null) {
            dir = parentDir.createDirectory(dirName);
        }

        return getParentDirectory(dir, path.substring(idx + 1));
    }

    static void createDirectories(DocumentFile parentDir, String dirPath) {
        int idx = dirPath.indexOf('/');
        String dirName = idx == -1 ? dirPath : dirPath.substring(0, idx);
        DocumentFile dir = parentDir.findFile(dirName);
        if (dir == null) {
            dir = parentDir.createDirectory(dirName);
        }
        if (idx != -1) {
            createDirectories(dir, dirPath.substring(idx + 1));
        }
    }
}
