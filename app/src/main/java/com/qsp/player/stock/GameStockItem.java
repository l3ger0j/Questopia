package com.qsp.player.stock;

import java.io.File;
import java.util.List;

import static com.qsp.player.shared.util.StringUtil.isNotEmpty;

public class GameStockItem implements Cloneable {
    public String id = "";
    public String listId = "";
    public String author = "";
    public String portedBy = "";
    public String version = "";
    public String title = "";
    public String lang = "";
    public String player = "";
    public String fileUrl = "";
    public int fileSize;
    public String fileExt = "";
    public String descUrl = "";
    public String pubDate = "";
    public String modDate = "";

    public File gameDir;
    public List<File> gameFiles;

    public GameStockItem() {
    }

    public GameStockItem(GameStockItem other) {
        id = other.id;
        listId = other.listId;
        author = other.author;
        portedBy = other.portedBy;
        version = other.version;
        title = other.title;
        lang = other.lang;
        player = other.player;
        fileUrl = other.fileUrl;
        fileSize = other.fileSize;
        fileExt = other.fileExt;
        descUrl = other.descUrl;
        pubDate = other.pubDate;
        modDate = other.modDate;
        gameDir = other.gameDir;
        gameFiles = other.gameFiles;
    }

    public boolean hasRemoteUrl() {
        return isNotEmpty(fileUrl);
    }

    public boolean isInstalled() {
        return gameDir != null;
    }
}
