package org.qp.android.viewModel.repository;

import static org.qp.android.helpers.utils.FileUtil.readFileAsString;
import static org.qp.android.helpers.utils.XmlUtil.xmlToObject;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.qp.android.dto.stock.InnerGameData;

import java.io.File;
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

    public List<InnerGameData> getGames(File gamesDir) {
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

    public List<InnerGameData> getGame(File gameDir) {
        if (gameDir == null) {
            Log.e(TAG,"Games directory is not specified");
            return Collections.emptyList();
        }
        var gameDirs = new ArrayList<File>();
        gameDirs.add(gameDir);
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
    }

    @Nullable
    private ArrayList<File> getGameDirectories(File gamesDir) {
        try {
            var dirs = new ArrayList<File>();
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
    private List<GameFolder> getGameFolders(List<File> dirs) {
        var folders = new ArrayList<GameFolder>();
        sortFilesByName(dirs);
        for (var dir : dirs) {
            var gameFiles = new ArrayList<File>();
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

    private void sortFilesByName(@NonNull List<File> files) {
        if (files.size() < 2) return;
        files.sort(Comparator.comparing(o -> o.getName().toLowerCase(Locale.ROOT)));
    }

    @Nullable
    private String getGameInfo(@NonNull GameFolder game) {
        var gameInfoFiles = game.dir.listFiles((dir, name) -> name.equalsIgnoreCase(GAME_INFO_FILENAME));
        if (gameInfoFiles == null || gameInfoFiles.length == 0) {
            Log.w(TAG, "InnerGameData info file not found in " + game.dir.getName());
            return null;
        }
        return readFileAsString(gameInfoFiles[0]);
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
        private final File dir;
        private final List<File> gameFiles;

        private GameFolder(File dir, List<File> gameFiles) {
            this.dir = dir;
            this.gameFiles = gameFiles;
        }
    }
}
