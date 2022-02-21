package com.qsp.player.install;

import static com.qsp.player.shared.util.FileUtil.createFile;
import static com.qsp.player.shared.util.FileUtil.getOrCreateDirectory;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.shared.util.StreamUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FolderGameInstaller extends GameInstaller {

    public FolderGameInstaller(Context context) {
        super(context);
    }

    @Override
    public boolean install(String gameName, DocumentFile srcFile, File destDir) {
        for (DocumentFile file : srcFile.listFiles()) {
            copyFileOrDirectory(file, destDir);
        }
        return postInstall(destDir);
    }

    private void copyFileOrDirectory(DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            File subDestDir = getOrCreateDirectory(destDir, srcFile.getName());
            for (DocumentFile subSrcFile : srcFile.listFiles()) {
                copyFileOrDirectory(subSrcFile, subDestDir);
            }
        } else {
            copyFile(srcFile, destDir);
        }
    }

    private void copyFile(DocumentFile srcFile, File destDir) {
        File destFile = createFile(destDir, srcFile.getName());
        if (destFile == null) {
            return;
        }
        try (InputStream in = context.getContentResolver().openInputStream(srcFile.getUri());
             OutputStream out = new FileOutputStream(destFile)) {
                StreamUtil.copy(in, out);
        } catch (IOException ex) {
            throw new InstallException("Error copying game files");
        }
    }
}
