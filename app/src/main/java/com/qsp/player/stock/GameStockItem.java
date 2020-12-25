package com.qsp.player.stock;

import java.io.File;
import java.util.List;

public class GameStockItem {
    public String gameId = "";
    public String listId = "";
    public String author = "";
    public String portedBy = "";
    public String version = "";
    public String title = "";
    public String lang = "";
    public String player = "";
    public String fileUrl = "";
    public int fileSize;
    public String descUrl = "";
    public String pubDate = "";
    public String modDate = "";
    public File gameDir;
    public List<File> gameFiles;

    public  boolean hasRemoteUrl() {
        return fileUrl != null && !fileUrl.isEmpty();
    }

    public boolean isInstalled() {
        return gameDir != null;
    }
}
