package com.qsp.player.model.install;

import static com.qsp.player.utils.ArchiveUtil.extractArchiveEntries;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.GameDirUtil.doesDirectoryContainGameFiles;
import static com.qsp.player.utils.GameDirUtil.normalizeGameDirectory;
import static com.qsp.player.utils.ViewUtil.showErrorDialog;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.utils.StreamUtil;
import com.qsp.player.utils.ViewUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class Installer {
    private final String TAG = this.getClass().getSimpleName();
    private final Context context;

    public Installer(Context context) {
        this.context = context;
    }

    public boolean gameInstall (String gameName, DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            return installDirectory(srcFile, destDir);
        } else {
            return installArchive(gameName, srcFile, destDir);
        }
    }

    protected boolean postInstall(File gameDir) {
        normalizeGameDirectory(gameDir);

        boolean containsGameFiles = doesDirectoryContainGameFiles(gameDir);
        if (!containsGameFiles) {
            ViewUtil.showErrorDialog(context, context.getString(R.string.noGameFilesError));
            return false;
        }

        return true;
    }

    private boolean installArchive (String gameName, DocumentFile srcFile, File destDir) {
        boolean extracted = false;
        ExecutorService service = Executors.newCachedThreadPool();
        Callable<Boolean> newTask = () -> extractArchiveEntries(context, srcFile.getUri(), destDir);
        FutureTask<Boolean> futureTask = new FutureTask<>(newTask);
        service.submit(futureTask);

        try {
            extracted = futureTask.get();
        } catch (ExecutionException | InterruptedException | OutOfMemoryError e) {
            Log.e(TAG, e.toString());
        }

        if (!extracted) {
            String message = context.getString(R.string.installError).replace("-GAMENAME-", gameName);
            showErrorDialog(context, message);
            return false;
        }
        return postInstall(destDir);
    }

    // TODO Deprecated! Rewrite to get .qsp file
    private boolean installDirectory(DocumentFile srcFile , File destDir) {
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
