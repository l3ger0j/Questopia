package com.qsp.player.install;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.shared.util.StreamUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.qsp.player.shared.util.FileUtil.createFile;
import static com.qsp.player.shared.util.FileUtil.getOrCreateDirectory;

public class FolderGameInstaller extends GameInstaller {
    private static final Logger logger = LoggerFactory.getLogger(FolderGameInstaller.class);

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
            File subDir = getOrCreateDirectory(parentDir, fileOrDir.getName());
            for (DocumentFile dirFile : fileOrDir.listFiles()) {
                copyFileOrDirectory(dirFile, subDir);
            }
        } else {
            copyFile(fileOrDir, parentDir);
        }
    }

    private void copyFile(DocumentFile file, File parentDir) {
        File destFile = createFile(parentDir, file.getName());
        if (destFile == null) {
            logger.error("Destination file is null");
            return;
        }
        try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
            try (OutputStream out = new FileOutputStream(destFile)) {
                StreamUtil.copy(in, out);
            }
        } catch (IOException ex) {
            throw new InstallException("Error copying game files");
        }
    }
}
