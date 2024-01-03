package org.qp.android.helpers.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.file.DocumentFileUtils;
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

    @Nullable
    public static byte[] getFileContents(@NonNull Context context ,
                                         @NonNull Uri uriContent) {
        var resolver = context.getContentResolver();
        try (var in = resolver.openInputStream(uriContent);
             var out = new ByteArrayOutputStream()) {
            if (in != null) {
                StreamUtil.copy(in , out);
            } else {
                throw new NullPointerException();
            }
            return out.toByteArray();
        } catch (Exception ex) {
            Log.e(TAG , "Error reading file: " + uriContent , ex);
            return null;
        }
    }

    public static void writeFileContents(@NonNull Context context ,
                                         @NonNull Uri uriContent ,
                                         byte[] dataToWrite) {
        var resolver = context.getContentResolver();
        try (var out = resolver.openOutputStream(uriContent , "w")) {
            if (out != null) {
                out.write(dataToWrite);
            } else {
                throw new IOException("Input is NULL!");
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to save the game state", ex);
        }

    }

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
        if (checkFile == null || !checkFile.exists()) {
            var tempFile = parentDir.createFile(mimeType , displayName);
            if (isWritableFile(tempFile)) {
                Log.i(TAG , "File created");
                return tempFile;
            }
        }
        return checkFile;
    }

    @Nullable
    public static DocumentFile createFindDFolder(DocumentFile parentDir ,
                                                 String displayName) {
        if (!isWritableDirectory(parentDir)) {
            return null;
        }

        var checkDir = parentDir.findFile(displayName);
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

    public static DocumentFile findFileOrDirectory(Context context ,
                                                   DocumentFile parentDir ,
                                                   final String name) {
        return DocumentFileUtils.child(parentDir , context , name);
    }

    @Nullable
    public static DocumentFile fromFullPath(@NonNull String fullPath ,
                                            @NonNull DocumentFile rootDir) {
        var findDir = rootDir;
        var nameGameDir = rootDir.getName();

        try {
            var index = fullPath.indexOf(nameGameDir);
            var subString = fullPath.substring(index);
            var splitString = subString.replace(nameGameDir + "/" , "");
            var pathToFileSegments = splitString.split("/");

            for (var segment : pathToFileSegments) {
                if (segment.isEmpty()) {
                    continue;
                }
                findDir = findDir.findFile(segment);
                if (findDir == null) {
                    break;
                }
            }
        } catch (NullPointerException i) {
            return null;
        }

        return findDir;
    }

    public static DocumentFile fromRelPath(@NonNull String relPath ,
                                           @NonNull DocumentFile rootDir) {
        var pathToFileSegments = relPath.split("/");
        var relFile = rootDir;

        for (var segment : pathToFileSegments) {
            if (segment.isEmpty()) {
                continue;
            }
            relFile = relFile.findFile(segment);
            if (relFile == null) {
                break;
            }
        }

        return relFile;
    }

    @Nullable
    public static String readFileAsString(Context context ,
                                          @Nullable Uri fileUri) {
        if (fileUri == null) return null;

        var result = new StringBuilder();
        var resolver = context.getContentResolver();

        try (var in = resolver.openInputStream(fileUri);
             var bufReader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ex) {
            Log.e(TAG , "Error reading a file" , ex);
            return null;
        }
        return result.toString();
    }

    @Nullable
    public static String readAssetFileAsString(Context context ,
                                               String fileName) {
        var result = new StringBuilder();
        var assetManager = context.getAssets();

        try (var in = assetManager.open(fileName);
             var bufReader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException ex) {
            Log.e(TAG , "Error reading a file" , ex);
            return null;
        }
        return result.toString();
    }

    public static void deleteDirectory(@NonNull DocumentFile delDir) {
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
            copyFileToDir(context , srcFile , destDir);
        }
    }

    public static void copyFileToDir(Context context ,
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
