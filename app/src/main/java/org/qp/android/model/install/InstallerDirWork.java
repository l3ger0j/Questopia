package org.qp.android.model.install;

import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.DirUtil.normalizeGameDirectory;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class InstallerDirWork extends Worker {
    private final File destDir = new File(Objects.requireNonNull(getInputData().getString("destDir")));
    private final DocumentFile srcDir = DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(getInputData().getString("srcDir")));

    public InstallerDirWork(@NonNull Context context , @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        var outputErrorTwo = new Data.Builder()
                .putString("errorTwo", "NFE")
                .build();

        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var task = service.submit(() -> {
            if (srcDir != null) {
                for (DocumentFile file : srcDir.listFiles()) {
                    copyFileOrDirectory(file , destDir);
                }
            }
        });
        try {
            task.get();
            normalizeGameDirectory(destDir);
            if (!doesDirectoryContainGameFiles(destDir)) {
                return Result.failure(outputErrorTwo);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return Result.success();
    }

    private void copyFileOrDirectory(@NonNull DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            File subDestDir = getOrCreateDirectory(destDir, srcFile.getName());
            for (DocumentFile subSrcFile : srcFile.listFiles()) {
                copyFileOrDirectory(subSrcFile, subDestDir);
            }
        } else {
            copyFile(getApplicationContext(), srcFile, destDir);
        }
    }
}
