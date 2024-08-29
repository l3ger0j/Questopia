package org.qp.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

import org.qp.android.model.lib.LibIProxy;
import org.qp.android.model.lib.LibProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;
import org.qp.android.ui.settings.SettingsController;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class QuestopiaApplication extends Application {

    public static final int UNPACK_GAME_NOTIFICATION_ID = 1800;
    public static final String UNPACK_GAME_CHANNEL_ID = "org.qp.android.channel.unpack_game";

    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer(this);
    private final LibProxyImpl libProxy = new LibProxyImpl(this);

    private DocumentFile currentGameDir;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    public void setCurrentGameDir(DocumentFile currentGameDir) {
        this.currentGameDir = currentGameDir;
    }

    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor
                .setCurGameDir(currentGameDir)
                .setController(SettingsController.newInstance(this));
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer.setCurGameDir(currentGameDir);
    }

    public LibIProxy getLibProxy() {
        return libProxy;
    }

    public DocumentFile getCurrentGameDir() {
        return currentGameDir;
    }

    public void createNotificationChannels() {
        var notificationManager = getSystemService(NotificationManager.class);
        var importance = NotificationManager.IMPORTANCE_DEFAULT;

        var name = getString(R.string.channelInstallGame);
        var channel = new NotificationChannel(UNPACK_GAME_CHANNEL_ID , name , importance);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        notificationManager.createNotificationChannel(channel);
    }

}