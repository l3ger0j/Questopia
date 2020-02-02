package com.qsp.player.stock;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.FileUtil;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ZipUtil {

    /**
     * Extracts the ZIP archive <code>zipFile</code> to a directory <code>gameDir</code>.
     */
    static void unzip(Context context, DocumentFile zipFile, DocumentFile gameDir) throws IOException {
        InputStream fileIn = context.getContentResolver().openInputStream(zipFile.getUri());
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
                DocumentFile file = parentDir.createFile("application/octet-stream", filename);
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
