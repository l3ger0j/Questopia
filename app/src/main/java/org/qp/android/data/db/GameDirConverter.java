package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

public class GameDirConverter {

    @TypeConverter
    public String fromGameDirUri(Uri gameDirUri) {
        return String.valueOf(gameDirUri);
    }

    @TypeConverter
    public Uri toGameDirUri(String data) {
        return Uri.parse(data);
    }

}
