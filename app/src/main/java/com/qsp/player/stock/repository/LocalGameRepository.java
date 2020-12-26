package com.qsp.player.stock.repository;

import com.qsp.player.stock.GameStockItem;
import com.qsp.player.util.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.qsp.player.util.FileUtil.GAME_INFO_FILENAME;

public class LocalGameRepository {
    private static final Logger logger = LoggerFactory.getLogger(LocalGameRepository.class);

    private File gamesDir;

    public void setGamesDirectory(File dir) {
        gamesDir = dir;
    }

    public List<GameStockItem> getGames() {
        if (gamesDir == null) {
            logger.error("Games directory is not specified");
            return Collections.emptyList();
        }

        ArrayList<File> gameDirs = getGameDirectories();
        if (gameDirs.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GameStockItem> items = new ArrayList<>();
        for (GameFolder folder : getGameFolders(gameDirs)) {
            String info = getGameInfo(folder);
            GameStockItem item;
            if (info != null) {
                item = parseGameInfo(info);
            } else {
                String name = folder.dir.getName();
                item = new GameStockItem();
                item.gameId = name;
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

        return FileUtil.readFileAsString(gameInfoFiles[0]);
    }

    private GameStockItem parseGameInfo(String xml) {
        GameStockItem result = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            boolean docStarted = false;
            String tagName = "";
            boolean gameStarted = false;
            GameStockItem item = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        docStarted = true;
                        break;

                    case XmlPullParser.START_TAG:
                        if (docStarted) {
                            tagName = xpp.getName();
                            if (tagName.equals("game")) {
                                gameStarted = true;
                                item = new GameStockItem();
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (docStarted && gameStarted) {
                            if (xpp.getName().equals("game")) {
                                result = item;
                            }
                            tagName = "";
                        }
                        break;

                    case XmlPullParser.CDSECT:
                        if (docStarted && gameStarted) {
                            fillGameItemFromCDATA(item, tagName, xpp.getText());
                        }
                        break;
                }

                eventType = xpp.nextToken();
            }
        } catch (XmlPullParserException | IOException e) {
            logger.error("Failed to parse game info file", e);
        }

        return result;
    }

    private void fillGameItemFromCDATA(GameStockItem item, String tagName, String value) {
        switch (tagName) {
            case "id":
                item.gameId = "id:".concat(value);
                break;
            case "list_id":
                item.listId = value;
                break;
            case "author":
                item.author = value;
                break;
            case "ported_by":
                item.portedBy = value;
                break;
            case "version":
                item.version = value;
                break;
            case "title":
                item.title = value;
                break;
            case "lang":
                item.lang = value;
                break;
            case "player":
                item.player = value;
                break;
            case "file_url":
                item.fileUrl = value;
                break;
            case "file_size":
                item.fileSize = Integer.parseInt(value);
                break;
            case "desc_url":
                item.descUrl = value;
                break;
            case "pub_date":
                item.pubDate = value;
                break;
            case "mod_date":
                item.modDate = value;
                break;
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