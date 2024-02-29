package org.qp.android.data.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.List;

@Entity(tableName = "game")
public class Game {

    @PrimaryKey()
    @NonNull
    public String id = "";
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

    @TypeConverters({GameDirConverter.class})
    public Uri gameDirUri;

    @TypeConverters({GameFilesConverter.class})
    public List<Uri> gameFilesUri;

}
