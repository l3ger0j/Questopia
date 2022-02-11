package com.qsp.player.util;

import static com.qsp.player.util.FileUtil.createDirectories;
import static com.qsp.player.util.FileUtil.createFile;
import static com.qsp.player.util.FileUtil.getParentDirectory;
import static com.qsp.player.util.PathUtil.getFilename;
import static com.qsp.player.util.PathUtil.removeTrailingSlash;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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
                    extractEntry(
                            entry.getName(),
                            entry.isDirectory(),
                            gameDir,
                            out -> StreamUtil.copy(zipIn, out));
                }
                return true;
            }
        } catch (Exception ex) {
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
                    final FileHeader fh = fileHeader;
                    extractEntry(
                            fh.getFileName(),
                            fh.isDirectory(),
                            gameDir,
                            out -> archive.extractFile(fh, out));
                }
                return true;
            }
        } catch (Exception ex) {
            logger.error("Failed to extract a RAR archive", ex);
            return false;
        }
    }

    private static void extractEntry(
            String entryName,
            boolean entryDir,
            File gameDir,
            ThrowingStreamWriter writer) throws Exception {
        String normEntryName = entryName.replace("\\", "/");
        if (entryDir) {
            createDirectories(gameDir, removeTrailingSlash(normEntryName));
            return;
        }
        File parentDir = getParentDirectory(gameDir, normEntryName);
        String filename = getFilename(normEntryName);
        File file = createFile(parentDir, filename);
        try (FileOutputStream out = new FileOutputStream(file)) {
            writer.accept(out);
        }
    }

    @FunctionalInterface
    private interface ThrowingStreamWriter {
        void accept(FileOutputStream stream) throws Exception;
    }
}
