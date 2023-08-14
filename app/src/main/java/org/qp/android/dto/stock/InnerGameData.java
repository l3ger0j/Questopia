package org.qp.android.dto.stock;

import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Root(name = "game", strict = false)
public class InnerGameData implements Serializable {
    @Element(name = "id", data = true)
    public String id = "";
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
    @Element(name = "icon", data = true, required = false)
    public String icon = "";
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

    public DocumentFile gameDir;
    public List<DocumentFile> gameFiles;

    public InnerGameData() {
    }

    public InnerGameData(InnerGameData other) {
        id = other.id;
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

    public String getFileSize() {
        return (fileSize != null) ? fileSize : "";
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
        var that = (InnerGameData) o;
        return Objects.equals(id, that.id)
                && Objects.equals(author, that.author)
                && Objects.equals(portedBy, that.portedBy)
                && Objects.equals(version, that.version)
                && Objects.equals(title, that.title)
                && Objects.equals(lang, that.lang)
                && Objects.equals(player, that.player)
                && Objects.equals(icon, that.icon)
                && Objects.equals(fileUrl, that.fileUrl)
                && Objects.equals(fileSize, that.fileSize)
                && Objects.equals(fileExt, that.fileExt)
                && Objects.equals(descUrl, that.descUrl)
                && Objects.equals(pubDate, that.pubDate)
                && Objects.equals(modDate, that.modDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id , author , portedBy , version , title , lang , player , icon , fileUrl , fileSize , fileExt , descUrl , pubDate , modDate);
    }

    @NonNull
    @Override
    public String toString() {
        return "InnerGameData{" +
                "id='" + id + '\'' +
                ", author='" + author + '\'' +
                ", portedBy='" + portedBy + '\'' +
                ", version='" + version + '\'' +
                ", title='" + title + '\'' +
                ", lang='" + lang + '\'' +
                ", player='" + player + '\'' +
                ", icon='" + icon + '\'' +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileSize='" + fileSize + '\'' +
                ", fileExt='" + fileExt + '\'' +
                ", descUrl='" + descUrl + '\'' +
                ", pubDate='" + pubDate + '\'' +
                ", modDate='" + modDate + '\'' +
                ", gameDir=" + gameDir +
                ", gameFiles=" + gameFiles +
                '}';
    }
}
