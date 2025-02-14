package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

public class UriConverter {

    @TypeConverter
    public String fromUri(Uri gameIconUri) {
        return gameIconUri == null ? "" : String.valueOf(gameIconUri);
    }

    @TypeConverter
    public Uri toUri(String data) {
        return data == null ? Uri.EMPTY : Uri.parse(data);
    }

}