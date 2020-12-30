package com.qsp.player;

import android.app.Application;
import android.os.Build;

import com.qsp.player.common.AudioPlayer;
import com.qsp.player.common.HtmlProcessor;
import com.qsp.player.common.ImageProvider;
import com.qsp.player.libqsp.LibQspProxy;
import com.qsp.player.libqsp.LibQspProxyImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.HandroidLoggerAdapter;

public class QuestPlayerApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(QuestPlayerApplication.class);

    private final HtmlProcessor htmlProcessor = new HtmlProcessor();
    private final ImageProvider imageProvider = new ImageProvider();
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQspProxyImpl libQspProxy = new LibQspProxyImpl(this, htmlProcessor, imageProvider, audioPlayer);

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
        logger.info("QuestPlayerApplication created");
    }

    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor;
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
    }
}
