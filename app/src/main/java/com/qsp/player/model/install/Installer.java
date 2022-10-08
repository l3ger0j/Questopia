package com.qsp.player.model.install;

import static com.qsp.player.utils.ArchiveUtil.extractArchiveEntries;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.DirUtil.doesDirectoryContainGameFiles;
import static com.qsp.player.utils.DirUtil.normalizeGameDirectory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.MutableLiveData;

import com.qsp.player.utils.StreamUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java9.util.concurrent.CompletableFuture;

public class Installer {
    private final MutableLiveData<Boolean> isDone = new MutableLiveData<>();
    private final Context context;

    public Installer(Context context) {
        this.context = context;
    }

    public MutableLiveData<Boolean> gameInstall (@NonNull DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            installDirectory(srcFile, destDir);
        } else {
            installArchive(srcFile, destDir);
        }
        return isDone;
    }

    protected void postInstall(File gameDir) {
        normalizeGameDirectory(gameDir);
        boolean containsGameFiles = doesDirectoryContainGameFiles(gameDir);
        if (!containsGameFiles) {
            isDone.postValue(false);
            throw new InstallException("NFE");
        }
        isDone.postValue(true);
    }

    private void installArchive (DocumentFile srcFile, File destDir) {
        CompletableFuture<Boolean> completableFuture =
                CompletableFuture
                        .supplyAsync(() -> extractArchiveEntries(
                                context, srcFile.getUri(), destDir))
                        .thenApply(aBoolean -> {
                            if (!aBoolean) {
                                isDone.postValue(false);
                                throw new InstallException("NIG");
                            } else {
                                isDone.postValue(true);
                                postInstall(destDir);
                                return true;
                            }
                        });
        completableFuture.isDone();
    }

    // TODO Deprecated! Rewrite to get .qsp file
    private void installDirectory(DocumentFile srcFile , File destDir) {
        for (DocumentFile file : srcFile.listFiles()) {
            copyFileOrDirectory(file, destDir);
        }
        postInstall(destDir);
    }

    public void copyFileOrDirectory(DocumentFile srcFile, File destDir) {
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
            throw new InstallException("CGF");
        }
    }
}
