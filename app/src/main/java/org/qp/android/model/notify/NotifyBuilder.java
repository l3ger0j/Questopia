package org.qp.android.model.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.qp.android.R;

public class NotifyBuilder {
    private final Context context;
    private final String CHANNEL_ID;

    private String titleNotify;
    private String textNotify;

    private NotificationCompat.Builder progressBuilder;

    public void setTitleNotify(String titleNotify) {
        this.titleNotify = titleNotify;
    }

    public void setTextNotify(String textNotify) {
        this.textNotify = textNotify;
    }

    public NotifyBuilder (Context context,
                          String CHANNEL_ID) {
        this.context = context;
        this.CHANNEL_ID = CHANNEL_ID;
    }

    public void createStatusChannel() {
        var notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        var notificationChannel = new NotificationChannel(
                "gameInstalled",
                context.getString(R.string.statusNameChannel),
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(context.getString(R.string.statusDescChannel));
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.GREEN);
        notificationChannel.enableVibration(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationChannel.setAllowBubbles(true);
        }
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public void createProgressChannel () {
        var notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        var notificationChannel = new NotificationChannel(
                "gameInstallationProgress",
                context.getString(R.string.progressNameChannel),
                NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setDescription(context.getString(R.string.progressDescChannel));
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(false);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public Notification buildWithProgress() {
        progressBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.add)
                .setContentTitle(titleNotify)
                .setContentText(textNotify)
                .setOngoing(true)
                .setProgress(0, 0, true);
        return progressBuilder.build();
    }

    public Notification updateProgress(int progress) {
        progressBuilder
                .setContentText(progress +" / "+ 100)
                .setProgress(100, progress, false);
        return progressBuilder.build();
    }

    public Notification buildWithoutProgress() {
       return new NotificationCompat.Builder(context, CHANNEL_ID)
               .setSmallIcon(R.drawable.add)
               .setContentTitle(titleNotify)
               .setContentText(textNotify)
               .setPriority(NotificationCompat.PRIORITY_DEFAULT)
               .build();
    }
}
