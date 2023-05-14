package org.qp.android.utils;

import static org.qp.android.utils.PathUtil.normalizeFolderName;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.qp.android.model.install.InstallException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public final class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();
    public static final String GAME_INFO_FILENAME = "gameStockInfo";

    public static <T> boolean isWritableFile(T file) {
        if (file == null) {
            return false;
        }
        if (file instanceof File) {
            var tempFile = (File) file;
            return tempFile.exists() && tempFile.canWrite();
        } else if (file instanceof DocumentFile) {
            var tempFile = (DocumentFile) file;
            return tempFile.exists() && tempFile.isFile() && tempFile.canWrite();
        }
        return false;
    }

    public static <T> boolean isWritableDirectory(T dir) {
        if (dir == null) {
            return false;
        }
        if (dir instanceof File) {
            var tempDir = (File) dir;
            return tempDir.exists() && tempDir.isDirectory() && tempDir.canWrite();
        } else if (dir instanceof DocumentFile) {
            var tempFile = (DocumentFile) dir;
            return tempFile.exists() && tempFile.isDirectory() && tempFile.canWrite();
        }
        return false;
    }

    public static File getOrCreateFile(File parentDir,
                                       String name) {
        var file = findFileOrDirectory(parentDir, name);
        if (file == null) {
            file = createFile(parentDir, name);
        }
        return file;
    }

    @Nullable
    public static File createFile(File parentDir,
                                  String name) {
        var file = new File(parentDir, name);
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
    public static File getOrCreateGameDirectory(File gamesDir,
                                         String gameName) {
        var folderName = normalizeFolderName(gameName);
        return getOrCreateDirectory(gamesDir , folderName);
    }

    @NonNull
    public static File getOrCreateDirectory(File parentDir,
                                            String name) {
        var dir = findFileOrDirectory(parentDir, name);
        if (dir == null) {
            dir = createDirectory(parentDir, name);
        }
        return dir;
    }

    @NonNull
    public static File createDirectory(File parentDir,
                                       String name) {
        var dir = new File(parentDir, name);
        if (dir.mkdir()) {
            Log.i(TAG,"Directory created");
        } else {
            Log.i(TAG,"Directory not created");
        }
        return dir;
    }

    @Nullable
    public static <T> T findFileOrDirectory(T parentDir,
                                            final String name) {
        if (parentDir instanceof File) {
            var tempDir = (File) parentDir;
            var files = tempDir.listFiles((dir, filename) -> filename.equalsIgnoreCase(name));
            if (files == null || files.length == 0) return null;
            return (T) files[0];
        } else if (parentDir instanceof DocumentFile) {
            var tempDir = (DocumentFile) parentDir;
            var found = tempDir.findFile(name);
            if (found == null || found.isVirtual()) return null;
            return (T) found;
        }
        return null;
    }

    @Nullable
    public static File findFileRecursively(File parentDir,
                                           String path) {
        var idx = path.indexOf("/");
        if (idx == -1) {
            return findFileOrDirectory(parentDir, path);
        }
        var dirName = path.substring(0, idx);
        var dir = findFileOrDirectory(parentDir, dirName);
        if (dir == null) return null;
        return findFileRecursively(dir, path.substring(idx + 1));
    }

    @Nullable
    public static String readFileAsString(File file) {
        var result = new StringBuilder();
        try (var in = new FileInputStream(file)) {
            var inReader = new InputStreamReader(in);
            try (var bufReader = new BufferedReader(inReader)) {
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
        if (dir.listFiles() == null) {
            return;
        }

        for (var file : dir.listFiles()) {
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

    @Nullable
    public static byte[] getFileContents(String path) {
        var file = new File(path);
        try (var in = new FileInputStream(file)) {
            try (var out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                return out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG,"Error reading file: " + path, ex);
            return null;
        }
    }

    public static long dirSize(@NonNull DocumentFile dir) {
        if (dir.exists()) {
            long result = 0;
            var fileList = dir.listFiles();
            for (var file : fileList) {
                if (file.isDirectory()) {
                    result += dirSize(file);
                } else {
                    result += file.length();
                }
            }
            return result;
        }
        return 0;
    }

    @NonNull
    public static String formatFileSize(long size,
                                        int numCountInfo) {
        if (size <= 0) {
            return "0";
        }
        String[] units = new String[0];
        if (numCountInfo == 1000) {
            units = new String[]{"B" , "KB" , "MB" , "GB" , "TB"};
        } else if (numCountInfo == 1024) {
            units = new String[]{"B" , "KiB" , "MiB" , "GiB" , "TiB"};
        }
        int digitGroups = (int) (Math.log10(size) / Math.log10(numCountInfo));
        return new DecimalFormat("#,##0.#").format(size /
                Math.pow(numCountInfo , digitGroups))
                + " " + units[digitGroups];
    }

    public static void copyFile(Context context,
                                @NonNull DocumentFile srcFile,
                                File destDir) {
        var destFile = createFile(destDir, srcFile.getName());
        if (destFile == null) {
            return;
        }
        try (var in = context.getContentResolver().openInputStream(srcFile.getUri());
             var out = new FileOutputStream(destFile)) {
            StreamUtil.copy(in, out);
        } catch (IOException ex) {
            throw new InstallException("CGF");
        }
    }
}
