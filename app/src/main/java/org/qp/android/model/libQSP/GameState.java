package org.qp.android.model.libQSP;

import java.io.File;
import java.util.ArrayList;

public class GameState {
    public final InterfaceConfiguration interfaceConfig = new InterfaceConfiguration();

    public boolean gameRunning;
    public String gameId;
    public String gameTitle;
    public File gameDir;
    public File gameFile;
    public String mainDesc = "";
    public String varsDesc = "";
    public ArrayList<QpListItem> actions = new ArrayList<>();
    public ArrayList<QpListItem> objects = new ArrayList<>();
    public ArrayList<QpMenuItem> menuItems = new ArrayList<>();

    public void reset() {
        interfaceConfig.reset();
        gameRunning = false;
        gameId = null;
        gameTitle = null;
        gameDir = null;
        gameFile = null;
        mainDesc = "";
        varsDesc = "";
        actions = new ArrayList<>();
        objects = new ArrayList<>();
        menuItems = new ArrayList<>();
    }
}
