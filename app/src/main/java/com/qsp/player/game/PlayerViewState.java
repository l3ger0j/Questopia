package com.qsp.player.game;

import java.io.File;
import java.util.ArrayList;

public class PlayerViewState {

    public boolean gameRunning;
    public String gameTitle;
    public File gameDir;
    public File gameFile;
    public boolean useHtml;
    public int fontSize;
    public int backColor;
    public int fontColor;
    public int linkColor;
    public String mainDesc = "";
    public String varsDesc = "";
    public ArrayList<QspListItem> actions = new ArrayList<>();
    public ArrayList<QspListItem> objects = new ArrayList<>();
    public ArrayList<QspMenuItem> menuItems = new ArrayList<>();

    public void reset() {
        gameRunning = false;
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
}
