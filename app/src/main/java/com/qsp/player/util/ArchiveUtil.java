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
import static com.qsp.player.util.FileUtil.getParentDirectory;
import static com.qsp.player.util.PathUtil.getFilename;
import static com.qsp.player.util.PathUtil.normalizeAbsolutePath;
import static com.qsp.player.util.PathUtil.normalizeDirectoryPath;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

public final class ArchiveUtil {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveUtil.class);

    /**
     * Распаковывает ZIP-архив <code>zipFile</code> в папку <code>gameDir</code>.
     */
    public static boolean unzip(Context context, DocumentFile zipFile, File gameDir) {
        try (InputStream in = context.getContentResolver().openInputStream(zipFile.getUri())) {
            try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(in, "cp866")) {
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
            logger.error("Failed to extract a ZIP archive", ex);
            return false;
        }
    }

    /**
     * Распаковывает RAR-архив <code>zipFile</code> в папку <code>gameDir</code>.
     */
    public static boolean unrar(Context context, DocumentFile rarFile, File gameDir) {
        try (InputStream in = context.getContentResolver().openInputStream(rarFile.getUri())) {
            try (Archive archive = new Archive(in)) {
                FileHeader fileHeader;
                while ((fileHeader = archive.nextFileHeader()) != null) {
                    String normalizedName = normalizeAbsolutePath(fileHeader.getFileName());
                    if (fileHeader.isDirectory()) {
                        createDirectories(gameDir, normalizeDirectoryPath(normalizedName));
                        continue;
                    }
                    File parentDir = getParentDirectory(gameDir, normalizedName);
                    String filename = getFilename(normalizedName);
                    File file = createFile(parentDir, filename);
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        archive.extractFile(fileHeader, out);
                    }
                }

                return true;
            }
        } catch (IOException | RarException ex) {
            logger.error("Failed to extract a RAR archive", ex);
            return false;
        }
    }
}
