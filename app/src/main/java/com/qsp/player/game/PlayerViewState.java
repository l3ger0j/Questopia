package com.qsp.player.game;

import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;

class PlayerViewState {

    boolean gameRunning;
    String gameTitle;
    DocumentFile gameDir;
    DocumentFile gameFile;
    boolean useHtml;
    int fontSize;
    int backColor;
    int fontColor;
    int linkColor;
    String mainDesc = "";
    String varsDesc = "";
    ArrayList<QspListItem> actions = new ArrayList<>();
    ArrayList<QspListItem> objects = new ArrayList<>();
    ArrayList<QspMenuItem> menuItems = new ArrayList<>();

    void reset() {
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
