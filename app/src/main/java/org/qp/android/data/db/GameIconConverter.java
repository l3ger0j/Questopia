package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

public class GameIconConverter {

    @TypeConverter
    public String fromGameIconUri(Uri gameIconUri) {
        if (gameIconUri == null) return "";
        return String.valueOf(gameIconUri);
    }

    @TypeConverter
    public Uri toGameIconUri(String data) {
        if (data == null) return Uri.EMPTY;
        return Uri.parse(data);
    }

}