package org.qp.android.model.install;

import static org.qp.android.utils.ArchiveUtil.extractArchiveEntries;
import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.DirUtil.normalizeGameDirectory;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class InstallerWork extends Worker {
    private final File destDir = new File(Objects.requireNonNull(getInputData().getString("destDir")));
    private final DocumentFile srcFile = DocumentFile.fromSingleUri(getApplicationContext(), Uri.parse(getInputData().getString("srcFile")));

    public InstallerWork(@NonNull Context context , @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data outputErrorOne = new Data.Builder()
                .putString("errorOne", "NIG")
                .build();

        Data outputErrorTwo = new Data.Builder()
                .putString("errorTwo", "NFE")
                .build();

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        FutureTask<Boolean> task = new FutureTask<>(() ->
                extractArchiveEntries(getApplicationContext() , Objects.requireNonNull(srcFile).getUri() , destDir));
        service.submit(task);
        try {
            if (task.get()) {
                normalizeGameDirectory(destDir);
                if (!doesDirectoryContainGameFiles(destDir)) {
                    return Result.failure(outputErrorTwo);
                }
            } else {
                return Result.failure(outputErrorOne);
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return Result.success();
    }
}
