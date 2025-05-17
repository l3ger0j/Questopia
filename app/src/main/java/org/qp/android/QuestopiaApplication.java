package org.qp.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;

import org.qp.android.model.plugin.PluginService;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class QuestopiaApplication extends Application {

    public static final int UNPACK_GAME_NOTIFICATION_ID = 1800;
    public static final String UNPACK_GAME_CHANNEL_ID = "org.qp.android.channel.unpack_game";
    public final AudioPlayer audioPlayer = new AudioPlayer();
    public final HtmlProcessor htmlProcessor = new HtmlProcessor();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        startService(new Intent(this, PluginService.class));
    }

    public void createNotificationChannels() {
        var notificationManager = getSystemService(NotificationManager.class);
        var importance = NotificationManager.IMPORTANCE_DEFAULT;

        var name = getString(R.string.channelInstallGame);
        var channel = new NotificationChannel(UNPACK_GAME_CHANNEL_ID, name, importance);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        notificationManager.createNotificationChannel(channel);
    }

}