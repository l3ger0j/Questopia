package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.List;

public class ListUriConverter {

    @TypeConverter
    public String fromListUri(List<Uri> gameFilesUri) {
        if (gameFilesUri == null) return "";
        var tempList = new ArrayList<String>();
        gameFilesUri.forEach(uri ->
                tempList.add(String.valueOf(uri))
        );
        return String.join(",", tempList);
    }

    @TypeConverter
    public List<Uri> toListUri(String data) {
        var inputList = data.split(",");
        var tempList = new ArrayList<Uri>();
        for (var s : inputList) {
            tempList.add(Uri.parse(s));
        }
        return tempList;
    }

}
