package com.qsp.player.stock;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.FileUtil;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class ZipUtil {

    /**
     * Extracts the ZIP archive <code>zipPath</code> to a directory <code>gameDirPath</code>.
     */
    static void unzip(String zipPath, String gameDirPath) throws IOException {
        FileInputStream fileIn = new FileInputStream(zipPath);
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fileIn, "cp866")) {
            byte[] b = new byte[8192];
            ZipArchiveEntry entry;
            while ((entry = zipIn.getNextZipEntry()) != null) {
                String path = gameDirPath.concat("/").concat(entry.getName());
                if (entry.isDirectory()) {
                    createDirectories(path);
                    continue;
                }
                try (FileOutputStream out = new FileOutputStream(path)) {
                    int bytesRead;
                    while ((bytesRead = zipIn.read(b)) > 0) {
                        out.write(b, 0, bytesRead);
                    }
                }
            }
        }
    }

    private static void createDirectories(String dirPath) {
        new File(dirPath).mkdirs();
    }

    /**
     * Extracts the ZIP archive <code>zipPath</code> to a directory <code>gameDir</code>.
     */
    static void unzip(String zipPath, DocumentFile gameDir, Context context) throws IOException {
        FileInputStream fileIn = new FileInputStream(zipPath);
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fileIn, "cp866")) {
            byte[] b = new byte[8192];
            ZipArchiveEntry entry;
            while ((entry = zipIn.getNextZipEntry()) != null) {
                if (entry.isDirectory()) {
                    FileUtil.createDirectories(gameDir, FileUtil.normalizeDirectoryPath(entry.getName()));
                    continue;
                }
                DocumentFile parentDir = FileUtil.getParentDirectory(gameDir, entry.getName());
                String filename = FileUtil.getFilename(entry.getName());
                String mimeType = FileUtil.getMimeType(filename);
                DocumentFile file = parentDir.createFile(mimeType, filename);
                try (OutputStream out = context.getContentResolver().openOutputStream(file.getUri())) {
                    int bytesRead;
                    while ((bytesRead = zipIn.read(b)) > 0) {
                        out.write(b, 0, bytesRead);
                    }
                }
            }
        }
    }
}
