package com.qsp.player.game;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.qsp.player.QuestPlayerApplication;
import com.qsp.player.game.libqsp.LibQspProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundService extends Service {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundService.class);

    private final LocalBinder binder = new LocalBinder();

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
        logger.info("BackgroundService destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
}
