package org.qp.android.helpers.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.file.MimeType;

import org.qp.android.model.workers.WorkerException;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

public final class FileUtil {
    private static final String TAG = FileUtil.class.getSimpleName();

    public static FileWrapper.Document documentWrap(DocumentFile inputFile) {
        return new FileWrapper.Document(inputFile);
    }

    public static <T> boolean isWritableFile(T file) {
        if (file == null) {
            return false;
        }
        if (file instanceof File tempFile) {
            return tempFile.exists() && tempFile.canWrite();
        } else if (file instanceof DocumentFile tempFile) {
            return tempFile.exists() && tempFile.isFile() && tempFile.canWrite();
        }
        return false;
    }

    public static <T> boolean isWritableDirectory(T dir) {
        if (dir == null) {
            return false;
        }
        if (dir instanceof File tempDir) {
            return tempDir.exists() && tempDir.isDirectory() && tempDir.canWrite();
        } else if (dir instanceof DocumentFile tempFile) {
            return tempFile.exists() && tempFile.isDirectory() && tempFile.canWrite();
        }
        return false;
    }

    @Nullable
    public static DocumentFile createFindDFile(DocumentFile parentDir ,
                                               String mimeType ,
                                               String displayName) {
        if (!isWritableDirectory(parentDir)) {
            return null;
        }

        var checkFile = parentDir.findFile(displayName);
        if (checkFile == null) {
            var tempFile = parentDir.createFile(mimeType , displayName);
            if (isWritableFile(tempFile)) {
                Log.i(TAG , "File created");
                return tempFile;
            }
        }
        return checkFile;
    }

    @Nullable
    public static File createFindFolder(File parentDir , String name) {
        if (!isWritableDirectory(parentDir)) {
            return null;
        }

        var checkDir = findFileOrDirectory(parentDir , name);
        if (checkDir == null) {
            var newDir = new File(parentDir , name);
            if (newDir.mkdir()) {
                Log.i(TAG , "Directory created");
                return newDir;
            } else {
                Log.i(TAG , "Directory not created");
                return null;
            }
        }
        return checkDir;
    }

    @Nullable
    public static DocumentFile createFindDFolder(DocumentFile parentDir ,
                                                 String displayName) {
        if (!isWritableDirectory(parentDir)) {
            return null;
        }

        var checkDir = findFileOrDirectory(parentDir , displayName);
        if (checkDir == null) {
            var tempDir = parentDir.createDirectory(displayName);
            if (isWritableDirectory(tempDir)) {
                Log.i(TAG , "Directory created");
                return tempDir;
            } else {
                Log.e(TAG , "Directory not created");
            }
        }
        return checkDir;
    }

    @Nullable
    public static File findFileOrDirectory(File parentDir , final String name) {
        var files = parentDir.listFiles((dir , filename) -> filename.equalsIgnoreCase(name));
        if (files == null || files.length == 0) return null;
        return files[0];
    }

    public static DocumentFile findFileOrDirectory(DocumentFile parentDir , final String name) {
        return parentDir.findFile(name);
    }

    @Nullable
    public static String readFileAsString(Context context , DocumentFile file) {
        var result = new StringBuilder();
        try (var in = context.getContentResolver().openInputStream(file.getUri())) {
            var inReader = new InputStreamReader(in);
            try (var bufReader = new BufferedReader(inReader)) {
                String line;
                while ((line = bufReader.readLine()) != null) {
                    result.append(line);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG , "Error reading a file" , ex);
            return null;
        }
        return result.toString();
    }

    public static <T> void deleteDirectory(T dir) {
        if (dir instanceof File delDir) {
            if (delDir.listFiles() != null) {
                for (var file : delDir.listFiles()) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (file.delete()) {
                            Log.i(TAG , "File delete");
                        } else {
                            Log.e(TAG , "File not delete");
                        }
                    }
                }
                if (delDir.delete()) {
                    Log.i(TAG , "Directory delete");
                } else {
                    Log.e(TAG , "Directory not delete");
                }
            }
        } else if (dir instanceof DocumentFile delDir) {
            try {
                for (var file : delDir.listFiles()) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (file.delete()) {
                            Log.i(TAG , "File delete");
                        } else {
                            Log.e(TAG , "File not delete");
                        }
                    }
                }
                if (delDir.delete()) {
                    Log.i(TAG , "Directory delete");
                } else {
                    Log.e(TAG , "Directory not delete");
                }
            } catch (UnsupportedOperationException e) {
                Log.e(TAG , "Error: " , e);
            }
        }
    }

    @Nullable
    public static byte[] getFileContents(@NonNull Context context ,
                                         @NonNull Uri uriContent) {
        var resolver = context.getContentResolver();
        try (var in = resolver.openInputStream(uriContent)) {
            try (var out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in , out);
                return out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG , "Error reading file: " + uriContent , ex);
            return null;
        }
    }

    @NonNull
    public static String formatFileSize(long size ,
                                        int numCountInfo) {
        if (size <= 0) {
            return "0";
        }
        var units = new String[0];
        if (numCountInfo == 1000) {
            units = new String[]{"B" , "KB" , "MB" , "GB" , "TB"};
        } else if (numCountInfo == 1024) {
            units = new String[]{"B" , "KiB" , "MiB" , "GiB" , "TiB"};
        }
        var digitGroups = (int) (Math.log10(size) / Math.log10(numCountInfo));
        return new DecimalFormat("#,##0.#").format(size /
                Math.pow(numCountInfo , digitGroups))
                + " " + units[digitGroups];
    }

    public static void copyFileOrDirectory(Context context ,
                                           @NonNull DocumentFile srcFile ,
                                           DocumentFile destDir) {
        if (srcFile.isDirectory()) {
            var subDestDir = createFindDFolder(destDir , srcFile.getName());
            for (var subSrcFile : srcFile.listFiles()) {
                copyFileOrDirectory(context , subSrcFile , subDestDir);
            }
        } else if (srcFile.isFile()) {
            copyFile(context , srcFile , destDir);
        }
    }

    public static void copyFile(Context context ,
                                @NonNull DocumentFile srcFile ,
                                @NonNull DocumentFile destDir) {
        var destFile = createFindDFile(destDir , MimeType.UNKNOWN , srcFile.getName());
        if (destFile == null) {
            return;
        }
        try (var in = context.getContentResolver().openInputStream(srcFile.getUri());
             var out = context.getContentResolver().openOutputStream(destFile.getUri())) {
            if (in != null) {
                StreamUtil.copy(in , out);
            } else {
                throw new IOException();
            }
        } catch (IOException ex) {
            throw new WorkerException("CGF");
        }
    }
}
