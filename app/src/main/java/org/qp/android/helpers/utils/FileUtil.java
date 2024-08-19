package org.qp.android.helpers.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.callback.FileCallback;
import com.anggrayudi.storage.file.CreateMode;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.file.FileUtils;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

    public static boolean isWritableFile(Context context, DocumentFile file) {
        if (file == null) return false;
        var canWrite = DocumentFileUtils.isWritable(file, context);
        return file.exists() && file.isFile() && canWrite;
    }

    public static boolean isWritableFile(Context context, File file) {
        if (file == null) return false;
        var canWrite = FileUtils.isWritable(file, context);
        return file.exists() && file.isFile() && canWrite;
    }

    public static boolean isWritableDir(Context context, DocumentFile dir) {
        if (dir == null) return false;
        var canWrite = DocumentFileUtils.isWritable(dir, context);
        return dir.exists() && dir.isDirectory() && canWrite;
    }

    public static boolean isWritableDir(Context context, File dir) {
        if (dir == null) return false;
        var canWrite = FileUtils.isWritable(dir, context);
        return dir.exists() && dir.isDirectory() && canWrite;
    }

    public static void forceCreateFile(Context context,
                                       DocumentFile srcDir,
                                       String name,
                                       String mimeType) {
        DocumentFileUtils.makeFile(srcDir, context, name, mimeType, CreateMode.SKIP_IF_EXISTS);
    }

    @Nullable
    public static DocumentFile findOrCreateFile(Context context,
                                                DocumentFile srcDir,
                                                String name,
                                                String mimeType) {
        return DocumentFileUtils.makeFile(srcDir, context, name, mimeType, CreateMode.REUSE);
    }

    @Nullable
    public static File findOrCreateFile(Context context,
                                        File srcDir,
                                        String name,
                                        String mimeType) {
        return FileUtils.makeFile(srcDir, context, name, mimeType, CreateMode.REUSE);
    }

    @Nullable
    public static DocumentFile findOrCreateFolder(Context context,
                                                  DocumentFile srcDir,
                                                  String name) {
        return DocumentFileUtils.makeFolder(srcDir, context, name, CreateMode.REUSE);
    }

    @Nullable
    public static File findOrCreateFolder(Context context,
                                          File srcDir,
                                          String name) {
        return FileUtils.makeFolder(srcDir, context, name, CreateMode.REUSE);
    }

    public static DocumentFile fromRelPath(@NonNull Context context ,
                                           @NonNull final String path,
                                           @NonNull DocumentFile parentDir) {
        return DocumentFileUtils.child(parentDir, context, path);
    }

    @NonNull
    @Contract("_, _ -> new")
    public static File fromRelPath(@NonNull final String path,
                                   @NonNull File parentDir) {
        return FileUtils.child(parentDir, path);
    }

    @Nullable
    public static DocumentFile fromFullPath(@NonNull Context context,
                                            @NonNull String fullPath,
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
                findDir = fromRelPath(context, segment, findDir);
                if (findDir == null) {
                    break;
                }
            }
        } catch (NullPointerException i) {
            return null;
        }

        return findDir;
    }

    @Nullable
    public static String readFileAsString(Context context ,
                                          Uri fileUri) {
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
    public static String readFileAsString(File file) {
        if (file == null) return null;

        var result = new StringBuilder();

        try (var in = new InputStreamReader(new FileInputStream(file));
             var bufReader = new BufferedReader(in)){
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

    public static void forceDelFile(@NonNull Context context ,
                                    @NonNull DocumentFile documentFile) {
        DocumentFileUtils.forceDelete(documentFile , context);
    }

    @NonNull
    public static String formatFileSize(long size, int numCountInfo) {
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

    @WorkerThread
    public static void copyFileToDir(@NonNull Context context,
                                     @NonNull DocumentFile srcFile,
                                     @NonNull DocumentFile destDir,
                                     @NonNull FileCallback fileCallback) {
        DocumentFileUtils.copyFileTo(srcFile, context, destDir, null, fileCallback);
    }
}
