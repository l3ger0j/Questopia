package org.qp.android.model.size;

import static org.qp.android.helpers.utils.DirUtil.calculateDirSize;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.anggrayudi.storage.file.DocumentFileCompat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class DirSizeWorker extends Worker {
    private DocumentFile srcDir;

    public DirSizeWorker(@NonNull Context context,
                         @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        var data = getInputData().getString("srcDir");
        if (data == null) return;
        srcDir = DocumentFileCompat.fromUri(getApplicationContext(), Uri.parse(data));
    }

    @NonNull
    @Override
    public Result doWork() {
        if (srcDir == null) {
            return Result.failure();
        }

        var service = Executors.newSingleThreadExecutor();
        var task = service.submit(() -> calculateDirSize(srcDir));

        try {
            if (task.get() != null) {
                var outputDirSize = new Data.Builder()
                        .putLong("dirSize", task.get())
                        .build();
                return Result.success(outputDirSize);
            }
        } catch (ExecutionException | InterruptedException e) {
            var outputData = new Data.Builder()
                    .putString("errorException", String.valueOf(e))
                    .build();
            return Result.failure(outputData);
        }
        return Result.failure();
    }
}
