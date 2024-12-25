package org.qp.android.helpers.utils;

import android.content.Context;

import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.file.FileUtils;

import java.io.File;
import java.util.Locale;

public final class DirUtil {

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

    public static boolean isDirContainsGameFile(Context context, DocumentFile dir) {
        if (!isWritableDir(context, dir)) return false;

        for (var file : dir.listFiles()) {
            var dirName = file.getName();
            if (dirName == null) return false;
            var lcName = dirName.toLowerCase(Locale.ROOT);
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam"))
                return true;
        }

        return false;
    }

    @WorkerThread
    public static long calculateDirSize(DocumentFile dir) {
        if (dir.exists()) {
            long result = 0;
            var fileList = dir.listFiles();
            for (var file : fileList) {
                if (file.isDirectory()) {
                    result += calculateDirSize(file);
                } else {
                    result += file.length();
                }
            }
            return result;
        }
        return 0;
    }

}
