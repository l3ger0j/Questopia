package org.qp.android.model.workers;

import static org.qp.android.helpers.utils.DirUtil.dirSize;

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

public class SizeFolderWorker extends Worker {
    private final DocumentFile srcDir = DocumentFileCompat.fromUri(getApplicationContext(), Uri.parse(getInputData().getString("srcDir")));

    public SizeFolderWorker(@NonNull Context context ,
                            @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (srcDir == null) {
            return Result.failure();
        }

        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var task = service.submit(() -> dirSize(srcDir));

        try {
            if (task.get() != null) {
                var outputDirSize = new Data.Builder()
                        .putLong("dirSize" , task.get())
                        .build();
                return Result.success(outputDirSize);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.d(this.getClass().getSimpleName() , "Error: " , e);
            return Result.failure();
        }
        return Result.success();
    }
}