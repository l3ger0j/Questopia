package com.qsp.player.stock.dto;

import static com.qsp.player.shared.util.StringUtil.isNotEmpty;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Root(name = "game", strict = false)
public class Game {
    @Element(name = "id", data = true)
    public String id = "";
    @Element(name = "list_id", data = true, required = false)
    public String listId = "";
    @Element(name = "author", data = true, required = false)
    public String author = "";
    @Element(name = "ported_by", data = true, required = false)
    public String portedBy = "";
    @Element(name = "version", data = true, required = false)
    public String version = "";
    @Element(name = "title", data = true, required = false)
    public String title = "";
    @Element(name = "lang", data = true, required = false)
    public String lang = "";
    @Element(name = "player", data = true, required = false)
    public String player = "";
    @Element(name = "file_url", data = true, required = false)
    public String fileUrl = "";
    @Element(name = "file_size", data = true, required = false)
    public String fileSize;
    @Element(name = "file_ext", data = true, required = false)
    public String fileExt = "";
    @Element(name = "desc_url", data = true, required = false)
    public String descUrl = "";
    @Element(name = "pub_date", data = true, required = false)
    public String pubDate = "";
    @Element(name = "mod_date", data = true, required = false)
    public String modDate = "";

    public File gameDir;
    public List<File> gameFiles;

    public Game() {
    }

    public Game(Game other) {
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

    public int getFileSize() {
        return (fileSize != null) ? Integer.parseInt(fileSize) : 0;
    }

    public boolean hasRemoteUrl() {
        return isNotEmpty(fileUrl);
    }

    public boolean isInstalled() {
        return gameDir != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game that = (Game) o;
        return Objects.equals(id, that.id)
                && Objects.equals(listId, that.listId)
                && Objects.equals(author, that.author)
                && Objects.equals(portedBy, that.portedBy)
                && Objects.equals(version, that.version)
                && Objects.equals(title, that.title)
                && Objects.equals(lang, that.lang)
                && Objects.equals(player, that.player)
                && Objects.equals(fileUrl, that.fileUrl)
                && Objects.equals(fileSize, that.fileSize)
                && Objects.equals(fileExt, that.fileExt)
                && Objects.equals(descUrl, that.descUrl)
                && Objects.equals(pubDate, that.pubDate)
                && Objects.equals(modDate, that.modDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, listId, author, portedBy, version, title, lang, player, fileUrl, fileSize, fileExt, descUrl, pubDate, modDate);
    }
}
