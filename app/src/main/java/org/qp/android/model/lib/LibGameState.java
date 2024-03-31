package org.qp.android.model.lib;

import androidx.documentfile.provider.DocumentFile;

import org.qp.android.dto.lib.LibListItem;
import org.qp.android.dto.lib.LibMenuItem;

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
    public ArrayList<LibListItem> actionsList = new ArrayList<>();
    public ArrayList<LibListItem> objectsList = new ArrayList<>();
    public ArrayList<LibMenuItem> menuItemsList = new ArrayList<>();


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
