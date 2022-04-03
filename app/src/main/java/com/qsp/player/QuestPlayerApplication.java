package com.qsp.player;

import android.app.Application;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;

import com.qsp.player.libqsp.LibQspProxy;
import com.qsp.player.libqsp.LibQspProxyImpl;
import com.qsp.player.game.service.AudioPlayer;
import com.qsp.player.game.service.GameContentResolver;
import com.qsp.player.game.service.HtmlProcessor;
import com.qsp.player.game.service.ImageProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.HandroidLoggerAdapter;

public class QuestPlayerApplication extends Application {

    private static final Logger logger = LoggerFactory.getLogger(QuestPlayerApplication.class);
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(gameContentResolver, imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQspProxyImpl libQspProxy = new LibQspProxyImpl(this, gameContentResolver, imageProvider, htmlProcessor, audioPlayer);

    public QuestPlayerApplication() {
        initLogging();
    }

    private void initLogging() {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT;
        HandroidLoggerAdapter.APP_NAME = "Quest Player";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        libQspProxy.start();
        logger.info("QuestPlayerApplication created");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        libQspProxy.stop();
        logger.info("QuestPlayerApplication terminated");
    }

    public GameContentResolver getGameContentResolver() {
        return gameContentResolver;
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
    }
}
