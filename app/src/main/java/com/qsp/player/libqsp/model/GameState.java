package com.qsp.player.libqsp.model;

import java.io.File;
import java.util.ArrayList;

public class GameState {
    private final InterfaceConfiguration interfaceConfig = new InterfaceConfiguration();

    private boolean gameRunning;
    private String gameId;
    private String gameTitle;
    private File gameDir;
    private File gameFile;
    private String mainDesc = "";
    private String varsDesc = "";
    private ArrayList<QspListItem> actions = new ArrayList<>();
    private ArrayList<QspListItem> objects = new ArrayList<>();
    private ArrayList<QspMenuItem> menuItems = new ArrayList<>();

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

    public InterfaceConfiguration getInterfaceConfig() {
        return interfaceConfig;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void setGameRunning(boolean gameRunning) {
        this.gameRunning = gameRunning;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public void setGameTitle(String gameTitle) {
        this.gameTitle = gameTitle;
    }

    public File getGameDir() {
        return gameDir;
    }

    public void setGameDir(File gameDir) {
        this.gameDir = gameDir;
    }

    public File getGameFile() {
        return gameFile;
    }

    public void setGameFile(File gameFile) {
        this.gameFile = gameFile;
    }

    public String getMainDesc() {
        return mainDesc;
    }

    public void setMainDesc(String mainDesc) {
        this.mainDesc = mainDesc;
    }

    public String getVarsDesc() {
        return varsDesc;
    }

    public void setVarsDesc(String varsDesc) {
        this.varsDesc = varsDesc;
    }

    public ArrayList<QspListItem> getActions() {
        return actions;
    }

    public void setActions(ArrayList<QspListItem> actions) {
        this.actions = actions;
    }

    public ArrayList<QspListItem> getObjects() {
        return objects;
    }

    public void setObjects(ArrayList<QspListItem> objects) {
        this.objects = objects;
    }

    public ArrayList<QspMenuItem> getMenuItems() {
        return menuItems;
    }
}
