package org.qp.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;
import androidx.room.Room;

import org.qp.android.data.db.GameDatabase;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.LibQpProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

public class QuestPlayerApplication extends Application {

    public static final int INSTALL_GAME_NOTIFICATION_ID = 1800;

    public static final String CHANNEL_INSTALL_GAME = "org.qp.android.channel.install_game";

    public static QuestPlayerApplication instance;
    private GameDatabase database;
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQpProxyImpl libQspProxy = new LibQpProxyImpl(this , htmlProcessor , audioPlayer);

    private DocumentFile currentGameDir;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        instance = this;
        database = Room.databaseBuilder(
                this ,
                GameDatabase.class ,
                "database"
        ).build();
    }

    public void setCurrentGameDir(DocumentFile currentGameDir) {
        this.currentGameDir = currentGameDir;
    }

    public HtmlProcessor getHtmlProcessor() {
        htmlProcessor.setContext(this);
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQpProxy getLibQspProxy() {
        return libQspProxy;
    }

    public DocumentFile getCurrentGameDir() {
        return currentGameDir;
    }

    public static QuestPlayerApplication getInstance() {
        return instance;
    }

    public GameDatabase getDatabase() {
        return database;
    }

    public void createNotificationChannels() {
        var notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        var importance = NotificationManager.IMPORTANCE_LOW;

        var name = getString(R.string.channelInstallGame);
        var channel = new NotificationChannel(CHANNEL_INSTALL_GAME , name , importance);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        notificationManager.createNotificationChannel(channel);
    }

}