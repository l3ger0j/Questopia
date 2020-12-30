package com.qsp.player;

import android.app.Application;
import android.os.Build;

import com.qsp.player.game.HtmlProcessor;
import com.qsp.player.game.ImageProvider;
import com.qsp.player.game.libqsp.LibQspProxy;
import com.qsp.player.game.libqsp.LibQspProxyImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.HandroidLoggerAdapter;

public class QuestPlayerApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(QuestPlayerApplication.class);

    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor();
    private final LibQspProxyImpl libQspProxy = new LibQspProxyImpl(this, imageProvider, htmlProcessor);

    public QuestPlayerApplication() {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT;
        HandroidLoggerAdapter.APP_NAME = "Quest Player";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        libQspProxy.init();
        logger.info("QuestPlayerApplication created");
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public HtmlProcessor getHtmlProcessor() {
        return htmlProcessor;
    }

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
    }
}
