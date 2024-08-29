package org.qp.android.model.notify;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.qp.android.R;

public class NotifyBuilder {

    private final Context context;
    private final String CHANNEL_ID;

    public NotifyBuilder (@NonNull Context context,
                          @NonNull String CHANNEL_ID) {
        this.context = context;
        this.CHANNEL_ID = CHANNEL_ID;
    }

    public Notification buildStandardNotification(@NonNull String titleNotify,
                                                  @NonNull String textNotify) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_file_upload_24)
                .setContentTitle(titleNotify)
                .setContentText(textNotify)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }
}
