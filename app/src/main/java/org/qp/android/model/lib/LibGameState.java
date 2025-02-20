package org.qp.android.model.lib;

import android.net.Uri;

import com.libqsp.jni.QSPLib;

import java.util.List;

public class LibGameState {

    public boolean gameRunning;
    public long gameId = 0L;
    public String gameTitle;
    public Uri gameDirUri;
    public Uri gameFileUri;
    public String mainDesc = "";
    public String varsDesc = "";
    public final LibIConfig interfaceConfig = new LibIConfig();
    public List<QSPLib.ListItem> actionsList = List.of();
    public List<QSPLib.ListItem> objectsList = List.of();

    public void reset() {
        interfaceConfig.reset();
        gameRunning = false;
        gameId = 0L;
        gameTitle = "";
        gameDirUri = Uri.EMPTY;
        gameFileUri = Uri.EMPTY;
        mainDesc = "";
        varsDesc = "";
        actionsList.clear();
        objectsList.clear();
    }

}
