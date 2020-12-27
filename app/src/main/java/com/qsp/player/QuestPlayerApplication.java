package com.qsp.player;

import android.app.Application;
import android.os.Build;

import com.qsp.player.game.libqsp.LibQspProxy;
import com.qsp.player.game.libqsp.LibQspProxyImpl;

import org.slf4j.impl.HandroidLoggerAdapter;

public class QuestPlayerApplication extends Application {

    private LibQspProxyImpl libQspProxy;

    public QuestPlayerApplication() {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG;
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT;
        HandroidLoggerAdapter.APP_NAME = "Quest Player";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        libQspProxy = new LibQspProxyImpl(this);
    }

    @Override
    public void onTerminate() {
        libQspProxy.close();
        super.onTerminate();
    }

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
    }
}
