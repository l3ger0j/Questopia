package com.qsp.player;

import android.app.Application;
import android.os.Build;

import com.qsp.player.model.service.AudioPlayer;
import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.model.service.HtmlProcessor;
import com.qsp.player.model.service.ImageProvider;
import com.qsp.player.model.libQSP.LibQspProxy;
import com.qsp.player.model.libQSP.LibQspProxyImpl;

import org.slf4j.impl.HandroidLoggerAdapter;

public class QuestPlayerApplication extends Application {
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