package org.qp.android.data.db;

import android.net.Uri;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameFilesConverter {

    @TypeConverter
    public String fromGameFilesUri(List<Uri> gameFilesUri) {
        var tempList = new ArrayList<String>();
        gameFilesUri.forEach(uri ->
                tempList.add(String.valueOf(uri))
        );
        return String.join("," , tempList);
    }

    @TypeConverter
    public List<Uri> toGameFilesUri(String data) {
        var inputList = Arrays.asList(data.split(","));
        var tempList = new ArrayList<Uri>();
        inputList.forEach(s ->
                tempList.add(Uri.parse(data))
        );
        return tempList;
    }

}
