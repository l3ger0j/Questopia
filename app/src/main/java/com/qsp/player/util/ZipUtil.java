package com.qsp.player.util;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.qsp.player.util.FileUtil.MIME_TYPE_BINARY;

public final class ZipUtil {

    private static final String TAG = ZipUtil.class.getName();

    /**
     * Extracts the ZIP archive <code>zipFile</code> to a directory <code>gameDir</code>.
     */
    public static boolean unzip(Context context, DocumentFile zipFile, DocumentFile gameDir) {
        try (InputStream in = context.getContentResolver().openInputStream(zipFile.getUri())) {
            try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(in, "cp866")) {
                byte[] b = new byte[8192];
                ZipArchiveEntry entry;
                while ((entry = zipIn.getNextZipEntry()) != null) {
                    if (entry.isDirectory()) {
                        FileUtil.createDirectories(gameDir, FileUtil.normalizeDirectoryPath(entry.getName()));
                        continue;
                    }
                    DocumentFile parentDir = FileUtil.getParentDirectory(gameDir, entry.getName());
                    String filename = FileUtil.getFilename(entry.getName());
                    DocumentFile file = parentDir.createFile(MIME_TYPE_BINARY, filename);
                    try (OutputStream out = context.getContentResolver().openOutputStream(file.getUri())) {
                        int bytesRead;
                        while ((bytesRead = zipIn.read(b)) > 0) {
                            out.write(b, 0, bytesRead);
                        }
                    }
                }

                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to extract a ZIP file", e);
            return false;
        }
    }
}
