package org.qp.android;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.LibQpProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

public class QuestPlayerApplication extends Application {

    public static final int INSTALL_GAME_NOTIFICATION_ID = 1800;

    public static final int POST_INSTALL_GAME_NOTIFICATION_ID = 1801;

    public static final String CHANNEL_INSTALL_GAME = "org.qp.android.channel.install_game";

    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(gameContentResolver, imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQpProxyImpl libQspProxy = new LibQpProxyImpl(this, gameContentResolver, htmlProcessor, audioPlayer);

    private DocumentFile customRootDir;
    private DocumentFile currentGameDir;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    public void setCurrentGameDir(DocumentFile currentGameDir) {
        this.currentGameDir = currentGameDir;
    }

    public void setCustomRootFolder(DocumentFile customRootDir) {
        this.customRootDir = customRootDir;
    }

    public GameContentResolver getGameContentResolver() {
        return gameContentResolver;
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

    public DocumentFile getCustomRootDir() {
        return customRootDir;
    }

    public DocumentFile getCurrentGameDir() {
        return currentGameDir;
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

    public DocumentFile fromFullPath(@NonNull String fullPath) {
        var findDir = getCustomRootDir();
        var nameGameDir = currentGameDir.getName();

        var index = fullPath.lastIndexOf(nameGameDir);
        var simplePath = fullPath.substring(index);
        var pathToSoundSegments = simplePath.split("/");

        for (var segment : pathToSoundSegments) {
            if (segment.isEmpty()) {
                continue;
            }
            findDir = findDir.findFile(segment);
            if (findDir == null) {
                break;
            }
        }

        return findDir;
    }

    public DocumentFile fromRelativePath(@NonNull String relPath) {
        var pathToFileSegments = relPath.split("/");
        var relFile = currentGameDir;

        for (var segment : pathToFileSegments) {
            if (segment.isEmpty()) {
                continue;
            }
            relFile = relFile.findFile(segment);
            if (relFile == null) {
                break;
            }
        }

        Log.d(this.getClass().getSimpleName() , relPath);

        return relFile;
    }
}