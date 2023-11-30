package org.qp.android.model.workers;

import static android.content.Context.NOTIFICATION_SERVICE;
import static org.qp.android.helpers.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.helpers.utils.DirUtil.normalizeGameDirectory;
import static org.qp.android.helpers.utils.FileUtil.copyFileOrDirectory;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.anggrayudi.storage.file.DocumentFileCompat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CopyFolderWorker extends Worker {
    private final DocumentFile destDir = DocumentFileCompat.fromUri(getApplicationContext() , Uri.parse(getInputData().getString("destDir")));
    private final DocumentFile srcDir = DocumentFileCompat.fromUri(getApplicationContext() , Uri.parse(getInputData().getString("srcDir")));

    private NotificationManager notificationManager;

    public CopyFolderWorker(@NonNull Context context ,
                            @NonNull WorkerParameters workerParams) {
        super(context , workerParams);
        notificationManager = (NotificationManager)
                context.getSystemService(NOTIFICATION_SERVICE);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (srcDir == null) {
            return Result.failure();
        }

        var outputError = new Data.Builder()
                .putString("errorTwo" , "NFE")
                .build();

        var service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        var task = CompletableFuture
                .runAsync(() -> {
                    for (var file : srcDir.listFiles()) {
                        copyFileOrDirectory(getApplicationContext() , file , destDir);
                    }
                } , service)
                .thenRunAsync(() ->
                        normalizeGameDirectory(getApplicationContext() , destDir) , service);

//        notificationManager.cancel(INSTALL_GAME_NOTIFICATION_ID);
//        var progress = "Starting Download";
//        setForegroundAsync(createForegroundInfo(progress));

        try {
            task.get();
            if (!doesDirectoryContainGameFiles(destDir)) {
                return Result.failure(outputError);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.d(this.getClass().getSimpleName() , "Error: " , e);
            return Result.failure();
        }

        return Result.success();
    }

//    @NonNull
//    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
//        // Build a notification using bytesRead and contentLength
//        var context = getApplicationContext();
//        String id = context.getString(R.string.notification_channel_id);
//        String title = context.getString(R.string.notification_title);
//        String cancel = context.getString(R.string.cancel_download);
//        // This PendingIntent can be used to cancel the worker
//        PendingIntent intent = WorkManager.getInstance(context).createCancelPendingIntent(getId());
//
//        NotifyBuilder builder = new NotifyBuilder(context , CHANNEL_INSTALL_GAME);
//
//        builder.buildStandardNotification();
//        Notification notification = new NotificationCompat.Builder(context, id)
//                .setContentTitle(title)
//                .setTicker(title)
//                .setSmallIcon(R.drawable.ic_work_notification)
//                .setOngoing(true)
//                // Add the cancel action to the notification which can
//                // be used to cancel the worker
//                .addAction(android.R.drawable.ic_delete, cancel, intent)
//                .build();
//
//        return new ForegroundInfo(notificationId, notification);
//    }
}