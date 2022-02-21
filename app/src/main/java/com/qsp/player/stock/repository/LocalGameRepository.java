package com.qsp.player.stock.repository;

import static com.qsp.player.shared.util.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.shared.util.FileUtil.readFileAsString;
import static com.qsp.player.shared.util.XmlUtil.xmlToObject;

import com.qsp.player.stock.dto.Game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LocalGameRepository {
    private static final Logger logger = LoggerFactory.getLogger(LocalGameRepository.class);

    private File gamesDir;

    public void setGamesDirectory(File dir) {
        gamesDir = dir;
    }

    public List<Game> getGames() {
        if (gamesDir == null) {
            logger.error("Games directory is not specified");
            return Collections.emptyList();
        }

        ArrayList<File> gameDirs = getGameDirectories();
        if (gameDirs.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<Game> items = new ArrayList<>();
        for (GameFolder folder : getGameFolders(gameDirs)) {
            String info = getGameInfo(folder);
            Game item;
            if (info != null) {
                item = parseGameInfo(info);
            } else {
                String name = folder.dir.getName();
                item = new Game();
                item.id = name;
                item.title = name;
            }
            item.gameDir = folder.dir;
            item.gameFiles = folder.gameFiles;
            items.add(item);
        }

        return items;
    }

    private ArrayList<File> getGameDirectories() {
        ArrayList<File> dirs = new ArrayList<>();
        for (File f : gamesDir.listFiles()) {
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }

        return dirs;
    }

    private List<GameFolder> getGameFolders(List<File> dirs) {
        ArrayList<GameFolder> folders = new ArrayList<>();
        sortFilesByName(dirs);

        for (File dir : dirs) {
            ArrayList<File> gameFiles = new ArrayList<>();

            List<File> files = Arrays.asList(dir.listFiles());
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

        Collections.sort(files, (o1, o2) -> o1.getName().toLowerCase()
                .compareTo(o2.getName().toLowerCase()));
    }

    private String getGameInfo(GameFolder game) {
        File[] gameInfoFiles = game.dir.listFiles((dir, name) -> name.equalsIgnoreCase(GAME_INFO_FILENAME));
        if (gameInfoFiles == null || gameInfoFiles.length == 0) {
            logger.warn("Game info file not found in " + game.dir.getName());
            return null;
        }

        return readFileAsString(gameInfoFiles[0]);
    }

    private Game parseGameInfo(String xml) {
        try {
            return xmlToObject(xml, Game.class);
        } catch (Exception ex) {
            logger.error("Failed to parse game info file", ex);
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
