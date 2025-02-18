package org.qp.android.model.lib;

import androidx.documentfile.provider.DocumentFile;

import org.libndkqsp.jni.NDKLib;

import java.util.ArrayList;

public class LibGameState {

    public boolean gameRunning;
    public String gameId;
    public String gameTitle;
    public DocumentFile gameDir;
    public DocumentFile gameFile;
    public String mainDesc = "";
    public String varsDesc = "";
    public final LibIConfig interfaceConfig = new LibIConfig();
    public ArrayList<NDKLib.ListItem> actionsList = new ArrayList<>();
    public ArrayList<NDKLib.ListItem> objectsList = new ArrayList<>();
    public ArrayList<NDKLib.ListItem> menuItemsList = new ArrayList<>();

    public void reset() {
        interfaceConfig.reset();
        gameRunning = false;
        gameId = null;
        gameTitle = null;
        gameDir = null;
        gameFile = null;
        mainDesc = "";
        varsDesc = "";
        actionsList = new ArrayList<>();
        objectsList = new ArrayList<>();
        menuItemsList = new ArrayList<>();
    }

}
