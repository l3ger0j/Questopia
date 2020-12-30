package com.qsp.player.stock;

import java.io.File;
import java.util.List;

import static com.qsp.player.util.StringUtil.isNotEmpty;

public class GameStockItem implements Cloneable {
    private String id = "";
    private String listId = "";
    private String author = "";
    private String portedBy = "";
    private String version = "";
    private String title = "";
    private String lang = "";
    private String player = "";
    private String fileUrl = "";
    private int fileSize;
    private String descUrl = "";
    private String pubDate = "";
    private String modDate = "";

    private File gameDir;
    private List<File> gameFiles;

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
        descUrl = other.descUrl;
        pubDate = other.pubDate;
        modDate = other.modDate;
        gameDir = other.gameDir;
        gameFiles = other.gameFiles;
    }

    public  boolean hasRemoteUrl() {
        return isNotEmpty(fileUrl);
    }

    public boolean isInstalled() {
        return gameDir != null;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getListId() {
        return listId;
    }

    public void setListId(String listId) {
        this.listId = listId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPortedBy() {
        return portedBy;
    }

    public void setPortedBy(String portedBy) {
        this.portedBy = portedBy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getDescUrl() {
        return descUrl;
    }

    public void setDescUrl(String descUrl) {
        this.descUrl = descUrl;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getModDate() {
        return modDate;
    }

    public void setModDate(String modDate) {
        this.modDate = modDate;
    }

    public File getGameDir() {
        return gameDir;
    }

    public void setGameDir(File gameDir) {
        this.gameDir = gameDir;
    }

    public List<File> getGameFiles() {
        return gameFiles;
    }

    public void setGameFiles(List<File> gameFiles) {
        this.gameFiles = gameFiles;
    }
}
