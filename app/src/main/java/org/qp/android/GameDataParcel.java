package org.qp.android;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class GameDataParcel implements Parcelable {
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
    public String fileSize;
    public String fileExt = "";
    public String descUrl = "";
    public String pubDate = "";
    public String modDate = "";

    public File gameDir;
    public List<File> gameFiles;

    public GameDataParcel() {
    }

    public GameDataParcel(GameDataParcel other) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (GameDataParcel) o;
        return Objects.equals(id, that.id)
                && Objects.equals(listId, that.listId)
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
        return Objects.hash(id, listId, author, portedBy, version, title, lang, player, icon, fileUrl, fileSize, fileExt, descUrl, pubDate, modDate);
    }

    public GameDataParcel(@NonNull Parcel in) {
        id = in.readString();
        listId = in.readString();
        author = in.readString();
        portedBy = in.readString();
        version = in.readString();
        title = in.readString();
        lang = in.readString();
        player = in.readString();
        icon = in.readString();
        fileUrl = in.readString();
        fileSize = in.readString();
        fileExt = in.readString();
        descUrl = in.readString();
        pubDate = in.readString();
        modDate = in.readString();
    }

    public static final Creator<GameDataParcel> CREATOR = new Creator<>() {
        @Override
        public GameDataParcel createFromParcel(Parcel in) {
            return new GameDataParcel(in);
        }

        @Override
        public GameDataParcel[] newArray(int size) {
            return new GameDataParcel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel , int i) {
        parcel.writeString(id);
        parcel.writeString(listId);
        parcel.writeString(author);
        parcel.writeString(portedBy);
        parcel.writeString(version);
        parcel.writeString(title);
        parcel.writeString(lang);
        parcel.writeString(player);
        parcel.writeString(icon);
        parcel.writeString(fileUrl);
        parcel.writeString(fileSize);
        parcel.writeString(fileExt);
        parcel.writeString(descUrl);
        parcel.writeString(pubDate);
        parcel.writeString(modDate);
    }
}