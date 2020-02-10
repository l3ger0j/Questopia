package com.qsp.player;

import android.app.Application;

import com.qsp.player.game.LibQspProxy;
import com.qsp.player.game.LibQspProxyImpl;

public class QuestPlayerApplication extends Application {

    private LibQspProxyImpl libQspProxy;

    public LibQspProxy getLibQspProxy() {
        return libQspProxy;
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
}
