package org.qp.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.documentfile.provider.DocumentFile;

import org.qp.android.dto.stock.InnerGameData;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.LibQpProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

import java.util.ArrayList;

public class QuestPlayerApplication extends Application {

    public static final int INSTALL_GAME_NOTIFICATION_ID = 1800;

    public static final int POST_INSTALL_GAME_NOTIFICATION_ID = 1801;

    public static final String CHANNEL_INSTALL_GAME = "org.qp.android.channel.install_game";

    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(gameContentResolver, imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQpProxyImpl libQspProxy = new LibQpProxyImpl(this, gameContentResolver, htmlProcessor, audioPlayer);

    private Bundle gameSaveMap = new Bundle();
    private ArrayList<InnerGameData> gameList = new ArrayList<>();

    private DocumentFile customRootDir;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    public void setGameSaveMap(Bundle gameSaveMap) {
        this.gameSaveMap = gameSaveMap;
    }

    public Bundle getGameSaveMap() {
        return gameSaveMap;
    }

    public void setCustomRootDir(DocumentFile customRootDir) {
        this.customRootDir = customRootDir;
    }

    public void setGameList(ArrayList<InnerGameData> gameListHashMap) {
        this.gameList = gameListHashMap;
    }

    public ArrayList<InnerGameData> getGameList() {
        return gameList;
    }

    public GameContentResolver getGameContentResolver() {
        return gameContentResolver;
    }

    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQpProxy getLibQspProxy() {
        return libQspProxy;
    }

    public DocumentFile getCustomRootDir() {
        return customRootDir;
    }

    public void createNotificationChannels() {
        var notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        var importance = NotificationManager.IMPORTANCE_LOW;

        var name = getString(R.string.channelInstallGame);
        var channel =  new NotificationChannel(CHANNEL_INSTALL_GAME, name, importance);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true);
        }
        notificationManager.createNotificationChannel(channel);

    }
}