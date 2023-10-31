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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class LocalGame {
    private final String TAG = this.getClass().getSimpleName();
    private static final String GAME_INFO_FILENAME = "gameStockInfo";

    private Context context;

    public List<InnerGameData> getGames(Context context , DocumentFile gamesDir) {
        this.context = context;
        if (gamesDir == null) {
            return Collections.emptyList();
        }
        var gameDirs = getGameDirectories(gamesDir);
        if (gameDirs != null) {
            if (gameDirs.isEmpty()) {
                return Collections.emptyList();
            }
            var items = new ArrayList<InnerGameData>();
            for (var folder : getGameFolders(gameDirs)) {
                var item = (InnerGameData) null;
                var info = getGameInfo(folder);
                if (info != null) {
                    item = parseGameInfo(info);
                }
                if (item == null) {
                    var name = folder.dir.getName();
                    item = new InnerGameData();
                    item.id = name;
                    item.title = name;
                }
                item.gameDir = folder.dir;
                item.gameFiles = folder.gameFiles;
                items.add(item);
            }
            return items;
        } else {
            Log.d(TAG , "game dir is null");
            return Collections.emptyList();
        }
    }

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
    private List<GameFolder> getGameFolders(List<DocumentFile> dirs) {
        var folders = new ArrayList<GameFolder>();
        sortFilesByName(dirs);
        for (var dir : dirs) {
            var gameFiles = new ArrayList<DocumentFile>();
            var files = Arrays.asList(Objects.requireNonNull(dir.listFiles()));
            sortFilesByName(files);
            for (var file : files) {
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }
            folders.add(new GameFolder(dir, gameFiles));
        }
        return folders;
    }

    private void sortFilesByName(@NonNull List<DocumentFile> files) {
        if (files.size() < 2) return;
        files.sort(Comparator.comparing(o -> o.getName().toLowerCase(Locale.ROOT)));
    }

    @Nullable
    private String getGameInfo(@NonNull GameFolder game) {
        var gameInfoFiles = findFileOrDirectory(game.dir , GAME_INFO_FILENAME);
        if (gameInfoFiles == null || gameInfoFiles.length() == 0) {
            Log.w(TAG, "InnerGameData info file not found in " + game.dir.getName());
            return null;
        }
        return readFileAsString(context , gameInfoFiles);
    }

    @Nullable
    private InnerGameData parseGameInfo(String xml) {
        try {
            return xmlToObject(xml, InnerGameData.class);
        } catch (Exception ex) {
            Log.e(TAG,"Failed to parse game info file", ex);
            return null;
        }
    }

    private static class GameFolder {
        private final DocumentFile dir;
        private final List<DocumentFile> gameFiles;

        private GameFolder(DocumentFile dir, List<DocumentFile> gameFiles) {
            this.dir = dir;
            this.gameFiles = gameFiles;
        }
    }
}