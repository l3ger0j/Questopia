package org.qp.android.helpers.utils;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.DocumentFileType;
import com.anggrayudi.storage.file.DocumentFileUtils;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class DirUtil {

    public static final String MOD_DIR_NAME = "mods";

    @WorkerThread
    public static boolean isDirContainsGameFile(@NonNull Context context,
                                                @NonNull Uri dirUri) {
        var targetDir = DocumentFileCompat.fromUri(context, dirUri);
        if (!isWritableDir(context, targetDir)) return false;
        var files = DocumentFileUtils.search(targetDir, true, DocumentFileType.FILE);
        for (var file : files) {
            var dirExtension = documentWrap(file).getExtension();
            var lcName = dirExtension.toLowerCase(Locale.ROOT);
            if (lcName.contains("qsp") || lcName.contains("gam")) return true;
        }
        return false;
    }

    public static boolean isModDirExist(@NonNull Context context,
                                        @NonNull Uri dirUri) {
        if (dirUri == Uri.EMPTY) {
            return false;
        }

        var targetDir = DocumentFileCompat.fromUri(context, dirUri);
        if (targetDir == null) return false;
        return isWritableDir(context, fromRelPath(context, MOD_DIR_NAME, targetDir));
    }

    public static List<String> getNamesDir(@NonNull Context context,
                                           @NonNull List<Uri> dirUris) {
        if (dirUris == Uri.EMPTY) {
            return Collections.emptyList();
        }

        return dirUris.stream()
                .map(uri -> DocumentFileCompat.fromUri(context, uri))
                .filter(d -> d != null && d.getName() != null)
                .map(DocumentFile::getName)
                .toList();
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
