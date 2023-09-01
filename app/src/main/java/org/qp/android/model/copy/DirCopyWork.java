package org.qp.android.model.copy;

import static org.qp.android.helpers.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.helpers.utils.DirUtil.normalizeGameDirectory;
import static org.qp.android.helpers.utils.FileUtil.copyFile;
import static org.qp.android.helpers.utils.FileUtil.createFindDFolder;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.anggrayudi.storage.file.DocumentFileCompat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class DirCopyWork extends Worker {
    private final DocumentFile destDir = DocumentFileCompat.fromUri(getApplicationContext() , Uri.parse(getInputData().getString("destDir")));
    private final DocumentFile srcDir = DocumentFileCompat.fromUri(getApplicationContext() , Uri.parse(getInputData().getString("srcDir")));

    public DirCopyWork(@NonNull Context context ,
                       @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (srcDir == null) {
            return Result.failure();
        }

        var outputErrorTwo = new Data.Builder()
                .putString("errorTwo" , "NFE")
                .build();

        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var task = service.submit(() -> {
            for (var file : srcDir.listFiles()) {
                copyFileOrDirectory(file , destDir);
            }
        });

        try {
            task.get();
            normalizeGameDirectory(getApplicationContext() , destDir);
            if (!doesDirectoryContainGameFiles(destDir)) {
                return Result.failure(outputErrorTwo);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.d(this.getClass().getSimpleName() , "Eror: " , e);
            return Result.failure();
        }
        return Result.success();
    }

    private void copyFileOrDirectory(@NonNull DocumentFile srcFile , DocumentFile destDir) {
        if (srcFile.isDirectory()) {
            var subDestDir = createFindDFolder(destDir , srcFile.getName());
            for (var subSrcFile : srcFile.listFiles()) {
                copyFileOrDirectory(subSrcFile , subDestDir);
            }
        } else if (srcFile.isFile()) {
            copyFile(getApplicationContext() , srcFile , destDir);
        }
    }
}