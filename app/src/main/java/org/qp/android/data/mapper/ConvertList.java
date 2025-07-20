package org.qp.android.data.mapper;

import android.net.Uri;

import org.qp.android.data.source.database.model.Game;
import org.qp.android.data.source.network.model.RemoteGameData;

import java.util.ArrayList;
import java.util.List;

public final class ConvertList {

    public static ArrayList<Game> remoteDataListToEntryList(List<RemoteGameData> in) {
        var listRemoteGameEntry = new ArrayList<Game>();
        in.forEach(item -> {
            var emptyEntry = new Game();
            emptyEntry.listId = 1;
            emptyEntry.author = item.author;
            emptyEntry.portedBy = item.portedBy;
            emptyEntry.version = item.version;
            emptyEntry.title = item.title;
            emptyEntry.lang = item.lang;
            emptyEntry.player = item.player;
            emptyEntry.gameIconUri = Uri.parse(item.icon);
            emptyEntry.fileUrl = item.fileUrl;
            emptyEntry.fileSize = item.fileSize;
            emptyEntry.fileExt = item.fileExt;
            emptyEntry.descUrl = item.descUrl;
            emptyEntry.pubDate = item.pubDate;
            emptyEntry.modDate = item.modDate;
            listRemoteGameEntry.add(emptyEntry);
        });
        return listRemoteGameEntry;
    }

}
