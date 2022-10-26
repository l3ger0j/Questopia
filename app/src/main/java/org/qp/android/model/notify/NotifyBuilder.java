package org.qp.android.model.notify;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.qp.android.R;

public class NotifyBuilder {
    private final Context context;
    private final String CHANNEL_ID;

    private String titleNotify;
    private String textNotify;

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannel () {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel =
                new NotificationChannel(CHANNEL_ID,
                        "Installation status",
                        NotificationManager.IMPORTANCE_DEFAULT );
        notificationChannel.setDescription("Channel for displaying information about the current installation status of the game");
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.GREEN);
        notificationChannel.enableVibration(false);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public Notification build() {
       return new NotificationCompat.Builder(context, CHANNEL_ID)
               .setSmallIcon(R.drawable.add)
               .setContentTitle(titleNotify)
               .setContentText(textNotify)
               .setPriority(NotificationCompat.PRIORITY_DEFAULT)
               .build();
    }
}
