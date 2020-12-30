package com.qsp.player;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.qsp.player.libqsp.LibQspProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundService extends Service {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundService.class);

    private final Binder binder = new Binder() {
    };

    private LibQspProxy libQspProxy;

    @Override
    public void onCreate() {
        super.onCreate();

        libQspProxy = ((QuestPlayerApplication) getApplication()).getLibQspProxy();
        libQspProxy.start();

        logger.info("BackgroundService created");
    }

    @Override
    public void onDestroy() {
        libQspProxy.stop();
        super.onDestroy();
        logger.info("BackgroundService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
