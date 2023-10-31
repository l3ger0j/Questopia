package org.qp.android.helpers.repository;

import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.FileUtil.readFileAsString;
import static org.qp.android.helpers.utils.XmlUtil.xmlToObject;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.qp.android.dto.stock.InnerGameData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocalGame {
    private final String TAG = this.getClass().getSimpleName();
    private static final String GAME_INFO_FILENAME = "gameStockInfo";

    private Context context;

    @Nullable
    private ArrayList<DocumentFile> getGameDirectories(DocumentFile gamesDir) {
        try {
            var dirs = new ArrayList<DocumentFile>();
            for (var f : gamesDir.listFiles()) {
                if (f.isDirectory()) {
                    dirs.add(f);
                }
            }
            return dirs;
        } catch (NullPointerException e) {
            Log.d(TAG , "Error: " , e);
            return null;
        }
    }

    @NonNull
    private List<GameFolder> getGamesFolders(@NonNull List<DocumentFile> dirs) {
        var folders = new ArrayList<GameFolder>();

        for (var dir : dirs) {
            var gameFiles = new ArrayList<DocumentFile>();
            var files = dir.listFiles();

            for (var file : files) {
                if (file.getName() == null) continue;
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }

            folders.add(new GameFolder(dir , gameFiles));
        }

        return folders;
    }

    @Nullable
    private String getGameInfo(@NonNull GameFolder game) {
        var gameInfoFiles = findFileOrDirectory(game.dir , GAME_INFO_FILENAME);
        if (gameInfoFiles == null || gameInfoFiles.length() == 0) {
            Log.w(TAG , "InnerGameData info file not found in " + game.dir.getName());
            return null;
        }
        return readFileAsString(context , gameInfoFiles);
    }

    public List<InnerGameData> extractGameDataFromFolder(Context context , DocumentFile gameFolder) {
        if (gameFolder == null) {
            return Collections.emptyList();
        }

        var gamesDirs = getGameDirectories(gameFolder);
        if (gamesDirs == null) {
            Log.d(TAG , "game dir is null");
            return Collections.emptyList();
        }
        if (gamesDirs.isEmpty()) {
            return Collections.emptyList();
        }

        this.context = context;

        var itemsGamesDirs = new ArrayList<InnerGameData>();
        var formatGamesDirs = getGamesFolders(gamesDirs);

        for (var folder : formatGamesDirs) {
            var item = (InnerGameData) null;
            var info = getGameInfo(folder);

            if (info != null) {
                item = parseGameInfo(info);
            }

            if (item == null) {
                var name = folder.dir.getName();
                if (name == null) return Collections.emptyList();
                item = new InnerGameData();

                item.id = name;
                item.title = name;
            }

            item.gameDir = folder.dir;
            item.gameFiles = folder.gameFiles;

            itemsGamesDirs.add(item);
        }

        return itemsGamesDirs;
    }

    @Nullable
    private InnerGameData parseGameInfo(String xml) {
        try {
            return xmlToObject(xml , InnerGameData.class);
        } catch (Exception ex) {
            Log.e(TAG , "Failed to parse game info file" , ex);
            return null;
        }
    }

    private record GameFolder(DocumentFile dir , List<DocumentFile> gameFiles) {
    }
}