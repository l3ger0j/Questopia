package com.qsp.player.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public final class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    public static final String GAME_INFO_FILENAME = "gamestockInfo";

    public static boolean isWritableFile(File file) {
        return file != null && file.exists() && file.canWrite();
    }

    public static boolean isWritableDirectory(File dir) {
        return dir != null && dir.exists() && dir.isDirectory() && dir.canWrite();
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
                if (file.createNewFile()) {
                    Log.i(TAG, "File created");
                }
            } catch (IOException ex) {
                Log.e(TAG , "Error creating a file: " + name, ex);
                return null;
            }
        }
        return file;
    }

    @NonNull
    public static File getOrCreateDirectory(File parentDir, String name) {
        File dir = findFileOrDirectory(parentDir, name);
        if (dir == null) {
            dir = createDirectory(parentDir, name);
        }
        return dir;
    }

    @NonNull
    public static File createDirectory(File parentDir, String name) {
        File dir = new File(parentDir, name);
        if (dir.mkdir()) {
            Log.i(TAG,"Directory created");
        } else {
            Log.i(TAG,"Directory not created");
        }
        return dir;
    }

    @Nullable
    public static File findFileOrDirectory(File parentDir, final String name) {
        File[] files = parentDir.listFiles((dir, filename) -> filename.equalsIgnoreCase(name));
        if (files == null || files.length == 0) return null;
        return files[0];
    }

    @Nullable
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

    @Nullable
    public static String readFileAsString(File file) {
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
            Log.e(TAG,"Error reading a file", ex);
            return null;
        }
        return result.toString();
    }

    public static void deleteDirectory(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                if (file.delete()) {
                    Log.i(TAG,"File delete");
                } else {
                    Log.e(TAG,"File not delete");
                }
            }
        }
        if (dir.delete()) {
            Log.i(TAG,"Directory delete");
        } else {
            Log.e(TAG,"Directory not delete");
        }
    }

    public static File getParentDirectory(File parentDir, String path) {
        int idx = path.indexOf('/');
        if (idx == -1) return parentDir;

        String dirName = path.substring(0, idx);
        File dir = findFileOrDirectory(parentDir, dirName);
        if (dir == null) {
            dir =  createDirectory(parentDir, dirName);
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

    @Nullable
    public static byte[] getFileContents(String path) {
        File file = new File(path);
        try (FileInputStream in = new FileInputStream(file)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                return out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG,"Error reading file: " + path, ex);
            return null;
        }
    }
}
