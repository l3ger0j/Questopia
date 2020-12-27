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

import static com.qsp.player.util.FileUtil.createDirectories;
import static com.qsp.player.util.FileUtil.createFile;
import static com.qsp.player.util.FileUtil.getFilename;
import static com.qsp.player.util.FileUtil.getParentDirectory;
import static com.qsp.player.util.FileUtil.normalizeDirectoryPath;

public final class ZipUtil {
    private static final Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * Распаковывает ZIP-архив <code>zipFile</code> в папку <code>gameDir</code>.
     */
    public static boolean unzip(Context context, DocumentFile zipFile, File gameDir) {
        try (InputStream in = context.getContentResolver().openInputStream(zipFile.getUri())) {
            try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(in, "cp866")) {
                byte[] b = new byte[8192];
                ZipArchiveEntry entry;
                while ((entry = zipIn.getNextZipEntry()) != null) {
                    if (entry.isDirectory()) {
                        createDirectories(gameDir, normalizeDirectoryPath(entry.getName()));
                        continue;
                    }
                    File parentDir = getParentDirectory(gameDir, entry.getName());
                    String filename = getFilename(entry.getName());
                    File file = createFile(parentDir, filename);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        StreamUtil.copy(zipIn, out);
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
