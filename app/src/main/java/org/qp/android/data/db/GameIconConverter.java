package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

public class GameIconConverter {

    @TypeConverter
    public String fromGameIconUri(Uri gameIconUri) {
        return String.valueOf(gameIconUri);
    }

    @TypeConverter
    public Uri toGameIconUri(String data) {
        return Uri.parse(data);
    }

}