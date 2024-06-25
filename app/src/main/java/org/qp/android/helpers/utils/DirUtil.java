package org.qp.android.helpers.utils;

import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import java.util.Locale;

public final class DirUtil {

    private static final String TAG = DirUtil.class.getSimpleName();

    public static boolean isDirContainsGameFile(DocumentFile dir) {
        if (dir == null) {
            return false;
        }

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
