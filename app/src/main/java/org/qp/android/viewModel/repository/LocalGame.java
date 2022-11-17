package org.qp.android.viewModel.repository;

import static org.qp.android.utils.FileUtil.GAME_INFO_FILENAME;
import static org.qp.android.utils.FileUtil.readFileAsString;
import static org.qp.android.utils.XmlUtil.xmlToObject;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.qp.android.dto.stock.GameData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LocalGame {
    private final String TAG = this.getClass().getSimpleName();

    public List<GameData> getGames(File gamesDir) {
        if (gamesDir == null) {
            Log.e(TAG,"Games directory is not specified");
            return Collections.emptyList();
        }

        ArrayList<File> gameDirs = getGameDirectories(gamesDir);
        if (gameDirs.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GameData> items = new ArrayList<>();
        for (GameFolder folder : getGameFolders(gameDirs)) {
            GameData item = null;
            String info = getGameInfo(folder);
            if (info != null) {
                item = parseGameInfo(info);
            }
            if (item == null) {
                String name = folder.dir.getName();
                item = new GameData();
                item.id = name;
                item.title = name;
            }
            item.gameDir = folder.dir;
            item.gameFiles = folder.gameFiles;
            items.add(item);
        }

        return items;
    }

    @NonNull
    private ArrayList<File> getGameDirectories(File gamesDir) {
        ArrayList<File> dirs = new ArrayList<>();
        for (File f : Objects.requireNonNull(gamesDir.listFiles())) {
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }
        return dirs;
    }

    @NonNull
    private List<GameFolder> getGameFolders(List<File> dirs) {
        ArrayList<GameFolder> folders = new ArrayList<>();
        sortFilesByName(dirs);

        for (File dir : dirs) {
            ArrayList<File> gameFiles = new ArrayList<>();

            List<File> files = Arrays.asList(Objects.requireNonNull(dir.listFiles()));
            sortFilesByName(files);

            for (File file : files) {
                String lcName = file.getName().toLowerCase();
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }

            folders.add(new GameFolder(dir, gameFiles));
        }

        return folders;
    }

    private void sortFilesByName(List<File> files) {
        if (files.size() < 2) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Collections.sort(files, Comparator.comparing(o -> o.getName().toLowerCase()));
        } else {
            Collections.sort(files, (o1, o2) -> o1.getName().toLowerCase()
                    .compareTo(o2.getName().toLowerCase()));
        }
    }

    @Nullable
    private String getGameInfo(GameFolder game) {
        File[] gameInfoFiles = game.dir.listFiles((dir, name) -> name.equalsIgnoreCase(GAME_INFO_FILENAME));
        if (gameInfoFiles == null || gameInfoFiles.length == 0) {
            Log.w(TAG, "GameData info file not found in " + game.dir.getName());
            return null;
        }
        return readFileAsString(gameInfoFiles[0]);
    }

    @Nullable
    private GameData parseGameInfo(String xml) {
        try {
            return xmlToObject(xml, GameData.class);
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