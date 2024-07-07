package org.qp.android.dto.stock;

import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;

import androidx.documentfile.provider.DocumentFile;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class GameData implements Serializable {

    public String id = "";
    public String listId = "";
    public String author = "";
    public String portedBy = "";
    public String version = "";
    public String title = "";
    public String lang = "";
    public String player = "";
    public String icon = "";
    public String fileUrl = "";
    public String fileSize = "";
    public String fileExt = "";
    public String descUrl = "";
    public String pubDate = "";
    public String modDate = "";

    @JsonIgnore
    public DocumentFile gameDir;
    @JsonIgnore
    public List<DocumentFile> gameFiles;

    @JsonGetter
    public String getFileSize() {
        return (fileSize != null) ? fileSize : "";
    }

    @JsonIgnore
    public boolean isHasRemoteUrl() {
        if (fileUrl == null) return false;
        return isNotEmpty(fileUrl);
    }

    @JsonIgnore
    public boolean isFileInstalled() {
        return gameDir != null;
    }

    public GameData() {}

    public GameData(GameData other) {
        id = other.id;
        listId = other.listId;
        author = other.author;
        portedBy = other.portedBy;
        version = other.version;
        title = other.title;
        lang = other.lang;
        player = other.player;
        icon = other.icon;
        fileUrl = other.fileUrl;
        fileSize = other.fileSize;
        fileExt = other.fileExt;
        descUrl = other.descUrl;
        pubDate = other.pubDate;
        modDate = other.modDate;
        gameDir = other.gameDir;
        gameFiles = other.gameFiles;
    }

    public GameData(RemoteGameData other) {
        id = other.id;
        listId = other.listId;
        author = other.author;
        portedBy = other.portedBy;
        version = other.version;
        title = other.title;
        lang = other.lang;
        player = other.player;
        icon = other.icon;
        fileUrl = other.fileUrl;
        fileSize = other.fileSize;
        fileExt = other.fileExt;
        descUrl = other.descUrl;
        pubDate = other.pubDate;
        modDate = other.modDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameData gameData)) return false;
        return Objects.equals(id , gameData.id)
                && Objects.equals(author , gameData.author)
                && Objects.equals(portedBy , gameData.portedBy)
                && Objects.equals(version , gameData.version)
                && Objects.equals(title , gameData.title)
                && Objects.equals(lang , gameData.lang)
                && Objects.equals(player , gameData.player)
                && Objects.equals(icon , gameData.icon)
                && Objects.equals(fileUrl , gameData.fileUrl)
                && Objects.equals(fileSize , gameData.fileSize)
                && Objects.equals(fileExt , gameData.fileExt)
                && Objects.equals(descUrl , gameData.descUrl)
                && Objects.equals(pubDate , gameData.pubDate)
                && Objects.equals(modDate , gameData.modDate)
                && Objects.equals(gameDir , gameData.gameDir)
                && Objects.equals(gameFiles , gameData.gameFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id , author , portedBy ,
                version , title , lang ,
                player , icon , fileUrl ,
                fileSize , fileExt , descUrl ,
                pubDate , modDate , gameDir , gameFiles);
    }
}
