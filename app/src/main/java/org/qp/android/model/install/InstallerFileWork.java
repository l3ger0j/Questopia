package org.qp.android.model.install;

import static org.qp.android.utils.ArchiveUtil.extractArchiveEntries;
import static org.qp.android.utils.ArchiveUtil.testArchiveEntries;
import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.DirUtil.normalizeGameDirectory;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Deprecated
public class InstallerFileWork extends Worker {
    private final File destDir = new File(Objects.requireNonNull(getInputData().getString("destDir")));
    private final DocumentFile srcFile = DocumentFile.fromSingleUri(getApplicationContext(), Uri.parse(getInputData().getString("srcFile")));
    private final String passwordForArchive = getInputData().getString("passwordForArchive");

    public InstallerFileWork(@NonNull Context context , @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (srcFile == null) {
            return Result.failure();
        }

        var outputErrorOne = new Data.Builder()
                .putString("errorOne", "NIG")
                .build();

        var outputErrorTwo = new Data.Builder()
                .putString("errorTwo", "NFE")
                .build();

        var outputErrorThree = new Data.Builder()
                .putString("errorThree", "PasswordNotFound")
                .build();

        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var test = new FutureTask<>(() ->
                testArchiveEntries(
                        getApplicationContext() ,
                        srcFile.getUri() ,
                        destDir));
        service.submit(test);

        try {
            if (test.get() != null) {
                if (test.get().toString().contains("Password is 'null'")) {
                    if (passwordForArchive != null && !passwordForArchive.isEmpty()) {
                        var taskWithPassword = new FutureTask<>(() ->
                                extractArchiveEntries(
                                        getApplicationContext() ,
                                        srcFile.getUri() ,
                                        destDir,
                                        passwordForArchive));
                        service.submit(taskWithPassword);
                        if (taskWithPassword.get()) {
                            normalizeGameDirectory(destDir);
                            if (!doesDirectoryContainGameFiles(destDir)) {
                                return Result.failure(outputErrorTwo);
                            }
                        } else {
                            return Result.failure(outputErrorOne);
                        }
                    } else {
                        return Result.failure(outputErrorThree);
                    }
                } else {
                    return Result.failure();
                }
            } else {
                var taskWithoutPassword = new FutureTask<>(() ->
                        extractArchiveEntries(
                                getApplicationContext() ,
                                srcFile.getUri() ,
                                destDir));
                service.submit(taskWithoutPassword);
                if (taskWithoutPassword.get()) {
                    normalizeGameDirectory(destDir);
                    if (!doesDirectoryContainGameFiles(destDir)) {
                        return Result.failure(outputErrorTwo);
                    }
                } else {
                    return Result.failure(outputErrorOne);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(this.getClass().getSimpleName() , "Error: ", e);
        }
        return Result.success();
    }
}
