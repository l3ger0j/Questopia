package org.qp.android.dto.stock;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GameData implements Serializable {

    public long id = 0L;
    public int listId = 1;
    public String author = "";
    public String portedBy = "";
    public String version = "";
    public String title = "";
    public String lang = "";
    public String player = "";
    public Uri iconUrl = Uri.EMPTY;
    public String fileUrl = "";
    public long fileSize = 0L;
    public String fileExt = "";
    public String descUrl = "";
    public String pubDate = "";
    public String modDate = "";

    @JsonIgnore
    public Uri gameDirUri = Uri.EMPTY;
    @JsonIgnore
    public List<Uri> gameFilesUri = Collections.emptyList();

    public GameData() { }

    public GameData(RemoteGameData other) {
        id = other.id;
        listId = other.listId;
        author = other.author;
        portedBy = other.portedBy;
        version = other.version;
        title = other.title;
        lang = other.lang;
        player = other.player;
        iconUrl = other.icon;
        fileUrl = other.fileUrl;
        fileSize = other.fileSize;
        fileExt = other.fileExt;
        descUrl = other.descUrl;
        pubDate = other.pubDate;
        modDate = other.modDate;
    }

    @NonNull
    @Override
    public String toString() {
        return "GameData{" +
                "id='" + id + '\'' +
                ", listId='" + listId + '\'' +
                ", author='" + author + '\'' +
                ", portedBy='" + portedBy + '\'' +
                ", version='" + version + '\'' +
                ", title='" + title + '\'' +
                ", lang='" + lang + '\'' +
                ", player='" + player + '\'' +
                ", icon='" + iconUrl + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileExt='" + fileExt + '\'' +
                ", descUrl='" + descUrl + '\'' +
                ", pubDate='" + pubDate + '\'' +
                ", modDate='" + modDate + '\'' +
                ", gameDir=" + gameDirUri +
                ", gameFiles=" + gameFilesUri +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameData gameData)) return false;
        return Objects.equals(id, gameData.id)
                && Objects.equals(author, gameData.author)
                && Objects.equals(portedBy, gameData.portedBy)
                && Objects.equals(version, gameData.version)
                && Objects.equals(title, gameData.title)
                && Objects.equals(lang, gameData.lang)
                && Objects.equals(player, gameData.player)
                && Objects.equals(iconUrl, gameData.iconUrl)
                && Objects.equals(fileUrl, gameData.fileUrl)
                && Objects.equals(fileSize, gameData.fileSize)
                && Objects.equals(fileExt, gameData.fileExt)
                && Objects.equals(descUrl, gameData.descUrl)
                && Objects.equals(pubDate, gameData.pubDate)
                && Objects.equals(modDate, gameData.modDate)
                && Objects.equals(gameDirUri, gameData.gameDirUri)
                && Objects.equals(gameFilesUri, gameData.gameFilesUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, portedBy,
                version, title, lang,
                player, iconUrl, fileUrl,
                fileSize, fileExt, descUrl,
                pubDate, modDate, gameDirUri, gameFilesUri);
    }
}
