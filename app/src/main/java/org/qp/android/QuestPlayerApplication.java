package org.qp.android;

import android.app.Application;

import org.qp.android.model.libQSP.LibQpProxy;
import org.qp.android.model.libQSP.LibQpProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

public class QuestPlayerApplication extends Application {
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(gameContentResolver, imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQpProxyImpl libQspProxy = new LibQpProxyImpl(this, gameContentResolver, htmlProcessor, audioPlayer);

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
}