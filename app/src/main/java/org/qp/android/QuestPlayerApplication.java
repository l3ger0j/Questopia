package org.qp.android;

import android.app.Application;
import android.os.Bundle;

import org.qp.android.dto.stock.GameData;
import org.qp.android.model.libQP.LibQpProxy;
import org.qp.android.model.libQP.LibQpProxyImpl;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

import java.util.ArrayList;

public class QuestPlayerApplication extends Application {
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor htmlProcessor = new HtmlProcessor(gameContentResolver, imageProvider);
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final LibQpProxyImpl libQspProxy = new LibQpProxyImpl(this, gameContentResolver, htmlProcessor, audioPlayer);

    private Bundle gameSaveMap = new Bundle();
    private ArrayList<GameData> gameList = new ArrayList<>();

    public void setGameSaveMap(Bundle gameSaveMap) {
        this.gameSaveMap = gameSaveMap;
    }

    public Bundle getGameSaveMap() {
        return gameSaveMap;
    }

    public void setGameList(ArrayList<GameData> gameListHashMap) {
        this.gameList = gameListHashMap;
    }

    public ArrayList<GameData> getGameList() {
        return gameList;
    }

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