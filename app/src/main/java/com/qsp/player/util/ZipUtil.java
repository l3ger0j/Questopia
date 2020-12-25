package com.qsp.player.util;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ZipUtil {
    private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * Extracts the ZIP archive <code>zipFile</code> to a directory <code>gameDir</code>.
     */
    public static boolean unzip(Context context, DocumentFile zipFile, File gameDir) {
        try (InputStream in = context.getContentResolver().openInputStream(zipFile.getUri())) {
            try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(in, "cp866")) {
                byte[] b = new byte[8192];
                ZipArchiveEntry entry;
                while ((entry = zipIn.getNextZipEntry()) != null) {
                    if (entry.isDirectory()) {
                        FileUtil.createDirectories(gameDir, FileUtil.normalizeDirectoryPath(entry.getName()));
                        continue;
                    }
                    File parentDir = FileUtil.getParentDirectory(gameDir, entry.getName());
                    String filename = FileUtil.getFilename(entry.getName());
                    File file = FileUtil.createFile(parentDir, filename);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        int bytesRead;
                        while ((bytesRead = zipIn.read(b)) > 0) {
                            out.write(b, 0, bytesRead);
                        }
                    }
                }

                return true;
            }
        } catch (IOException ex) {
            logger.error("Failed to extract a ZIP file", ex);
            return false;
        }
    }
}
