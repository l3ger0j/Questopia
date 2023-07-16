package org.qp.android.model.notify;

import android.app.Notification;
import android.content.Context;

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

    public Notification buildStandardNotification() {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.add)
                .setContentTitle(titleNotify)
                .setContentText(textNotify)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }
}
