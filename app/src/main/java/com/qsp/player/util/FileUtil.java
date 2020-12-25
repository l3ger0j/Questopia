package com.qsp.player.util;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

public final class FileUtil {

    public static final String GAME_INFO_FILENAME = "gamestockInfo";

    private static final String TAG = FileUtil.class.getName();

    public static boolean isWritableFile(File file) {
        return file != null && file.exists() && file.canWrite();
    }

    public static boolean isWritableDirectory(File dir) {
        return dir != null && dir.exists() && dir.isDirectory() && dir.canWrite();
    }

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

    public static File getOrCreateFile(File parentDir, String name) {
        File file = findFileOrDirectory(parentDir, name);
        if (file == null) {
            file = createFile(parentDir, name);
        }
        return file;
    }

    public static File createFile(File parentDir, String name) {
        File file = new File(parentDir, name);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating a file: " + name, ex);
                return null;
            }
        }
        return file;
    }

    public static File getOrCreateDirectory(File parentDir, String name) {
        File dir = findFileOrDirectory(parentDir, name);
        if (dir == null) {
            dir = createDirectory(parentDir, name);
        }
        return dir;
    }

    public static File createDirectory(File parentDir, String name) {
        File dir = new File(parentDir, name);
        dir.mkdir();
        return dir;
    }

    public static File findFileOrDirectory(File parentDir, final String name) {
        File[] files = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.equalsIgnoreCase(name);
            }
        });
        if (files == null || files.length == 0) return null;

        return files[0];
    }

    public static File findFileRecursively(File parentDir, String path) {
        int idx = path.indexOf("/");
        if (idx == -1) {
            return findFileOrDirectory(parentDir, path);
        }

        String dirName = path.substring(0, idx);
        File dir = findFileOrDirectory(parentDir, dirName);
        if (dir == null) return null;

        return findFileRecursively(dir, path.substring(idx + 1));
    }

    public static String readFileAsString(Context context, File file) {
        StringBuilder result = new StringBuilder();
        try (FileInputStream in = new FileInputStream(file)) {
            InputStreamReader inReader = new InputStreamReader(in);
            try (BufferedReader bufReader = new BufferedReader(inReader)) {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    result.append(line);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error reading a file", ex);
            return null;
        }
        return result.toString();
    }

    public static void deleteDirectory(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
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

    public static File getParentDirectory(File parentDir, String path) {
        int idx = path.indexOf('/');
        if (idx == -1) return parentDir;

        String dirName = path.substring(0, idx);
        File dir = findFileOrDirectory(parentDir, dirName);
        if (dir == null) {
            dir =  createDirectory(parentDir, dirName);;
        }

        return getParentDirectory(dir, path.substring(idx + 1));
    }

    public static void createDirectories(File parentDir, String dirPath) {
        int idx = dirPath.indexOf('/');
        String dirName = idx == -1 ? dirPath : dirPath.substring(0, idx);
        File dir = findFileOrDirectory(parentDir, dirName);
        if (dir == null) {
            dir = createDirectory(parentDir, dirName);
        }
        if (idx != -1) {
            createDirectories(dir, dirPath.substring(idx + 1));
        }
    }

    public static byte[] getFileContents(String path) {
        File file = new File(path);
        try (FileInputStream in = new FileInputStream(file)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int bytesRead;
                do {
                    bytesRead = in.read(buf);
                    if (bytesRead > 0) {
                        out.write(buf, 0, bytesRead);
                    }
                } while (bytesRead != -1);

                return out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error reading file: " + path, ex);
            return null;
        }
    }
}
