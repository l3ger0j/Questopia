package com.qsp.player.stock.install;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FolderGameInstaller extends GameInstaller {

    private static final String TAG = FolderGameInstaller.class.getName();
    private static final int BUFFER_SIZE = 8192;

    public FolderGameInstaller(Context context) {
        super(context);
    }

    @Override
    public void load(Uri uri) {
        gameFileOrDir = DocumentFile.fromTreeUri(context, uri);
        if (gameFileOrDir == null || !gameFileOrDir.exists()) {
            throw new InstallException("Folder not found: " + uri);
        }
        gameName = gameFileOrDir.getName();
    }

    @Override
    public boolean install(File gameDir) {
        for (DocumentFile file : gameFileOrDir.listFiles()) {
            copyFileOrDirectory(file, gameDir);
        }
        return postInstall(gameDir);
    }

    private void copyFileOrDirectory(DocumentFile fileOrDir, File parentDir) {
        if (fileOrDir.isDirectory()) {
            File subDir = FileUtil.getOrCreateDirectory(parentDir, fileOrDir.getName());
            for (DocumentFile dirFile : fileOrDir.listFiles()) {
                copyFileOrDirectory(dirFile, subDir);
            }
        } else {
            copyFile(fileOrDir, parentDir);
        }
    }

    private void copyFile(DocumentFile file, File parentDir) {
        File destFile = FileUtil.createFile(parentDir, file.getName());
        if (destFile == null) {
            Log.e(TAG, "Destination file is null");
            return;
        }
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
            try (OutputStream out = new FileOutputStream(destFile)) {
                int bytesRead;
                do {
                    bytesRead = in.read(buffer);
                    if (bytesRead > 0) {
                        out.write(buffer, 0, bytesRead);
                    }
                } while (bytesRead > 0);
            }
        } catch (IOException ex) {
            throw new InstallException("Error copying game files");
        }
    }
}
