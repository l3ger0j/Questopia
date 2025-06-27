package org.qp.android.helpers.utils;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileType;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.file.FileUtils;
import com.anggrayudi.storage.file.MimeType;

import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

    @NonNull
    @WorkerThread
    public static @Unmodifiable List<Uri> receiveGameFilesFromDir(Context context, DocumentFile gameDir) {
        if (!isWritableDir(context, gameDir)) return List.of();
        final var files = gameDir.listFiles();
        if (files.length == 0) return List.of();

        final var gameFiles = new ArrayList<Uri>();
        for (var documentFile : files) {
            if (documentFile.getName() == null) continue;
            var lcName = documentFile.getName().toLowerCase(Locale.ROOT);
            if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                gameFiles.add(documentFile.getUri());
            }
        }

        if (gameFiles.isEmpty()) {
            var allFiles = DocumentFileUtils.search(
                    gameDir,
                    true,
                    DocumentFileType.FILE,
                    new String[]{MimeType.BINARY_FILE}
            );

            for (var documentFile : allFiles) {
                if (documentFile.getName() == null) continue;
                var lcName = documentFile.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(documentFile.getUri());
                }
            }
        }

        return gameFiles;
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
