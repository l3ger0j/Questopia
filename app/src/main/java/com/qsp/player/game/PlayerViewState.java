package com.qsp.player.game;

import java.io.File;
import java.util.ArrayList;

public class PlayerViewState {
    private boolean gameRunning;
    private String gameId;
    private String gameTitle;
    private File gameDir;
    private File gameFile;
    private boolean useHtml;
    private int fontSize;
    private int backColor;
    private int fontColor;
    private int linkColor;
    private String mainDesc = "";
    private String varsDesc = "";
    private ArrayList<QspListItem> actions = new ArrayList<>();
    private ArrayList<QspListItem> objects = new ArrayList<>();
    private ArrayList<QspMenuItem> menuItems = new ArrayList<>();

    public void reset() {
        gameRunning = false;
        gameId = null;
        gameTitle = null;
        gameDir = null;
        gameFile = null;
        useHtml = false;
        fontSize = 0;
        backColor = 0;
        fontColor = 0;
        linkColor = 0;
        mainDesc = "";
        varsDesc = "";
        actions = new ArrayList<>();
        objects = new ArrayList<>();
        menuItems = new ArrayList<>();
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

    public boolean isUseHtml() {
        return useHtml;
    }

    public void setUseHtml(boolean useHtml) {
        this.useHtml = useHtml;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getBackColor() {
        return backColor;
    }

    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    public int getFontColor() {
        return fontColor;
    }

    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
    }

    public int getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(int linkColor) {
        this.linkColor = linkColor;
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

    public void setMenuItems(ArrayList<QspMenuItem> menuItems) {
        this.menuItems = menuItems;
    }
}
