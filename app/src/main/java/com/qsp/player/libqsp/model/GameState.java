package com.qsp.player.libqsp.model;

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
    public ArrayList<QspListItem> actions = new ArrayList<>();
    public ArrayList<QspListItem> objects = new ArrayList<>();
    public ArrayList<QspMenuItem> menuItems = new ArrayList<>();

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
