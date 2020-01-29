package com.qsp.player.stock;

import android.content.Context;
import android.text.TextUtils;

import com.qsp.player.R;
import com.qsp.player.Utility;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.qsp.player.stock.QspGameStock.GAME_INFO_FILENAME;

class LocalGameRepository {

    private final Context context;

    LocalGameRepository(Context context) {
        this.context = context;
    }

    Collection<GameItem> getGameItems() {
        String gamesPath = Utility.GetGamesPath(context);
        File gamesDir = TextUtils.isEmpty(gamesPath) ? null : new File(gamesPath);
        String downloadPath = Utility.GetDownloadPath(context);
        File downloadDir = TextUtils.isEmpty(downloadPath) || downloadPath.equals(gamesPath) ? null : new File(downloadPath);
        if (gamesDir == null && downloadDir == null) {
            throw new NoCardAccessException();
        }

        List<File> subdirectories = getSubdirectories(gamesDir, downloadDir);
        if (subdirectories.isEmpty()) {
            return Collections.emptyList();
        }

        Collection<GameItem> items = new ArrayList<>();

        for (Game game : getGames(subdirectories)) {
            String info = getGameInfo(game);
            boolean gamePack = game.gameFiles.size() > 1;
            for (File file : game.gameFiles) {
                GameItem item;
                if (info != null) {
                    item = parseGameInfo(info);
                } else {
                    String name = game.dir.getName();
                    item = new GameItem();
                    item.title = name;
                    item.game_id = name;
                }
                if (gamePack) {
                    String name = file.getName();
                    item.title += " (" + name + ")";
                    item.game_id += " (" + name + ")";
                }
                item.game_file = file.getPath();
                item.downloaded = true;
                items.add(item);
            }
        }

        return items;
    }

    private List<File> getSubdirectories(File... dirs) {
        List<File> sub = new ArrayList<>();
        for (File dir : dirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                sub.addAll(getSubdirectories(dir));
            }
        }

        return sub;
    }

    private Collection<File> getSubdirectories(File dir) {
        Collection<File> sub = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                sub.add(f);
            }
        }

        return sub;
    }

    private List<Game> getGames(List<File> dirs) {
        List<Game> games = new ArrayList<>();
        Utility.FileSorter(dirs);

        for (File dir : dirs) {
            if (!dir.isHidden() && !dir.getName().startsWith(".")) {
                List<File> gameFiles = new ArrayList<>();

                List<File> files = Arrays.asList(dir.listFiles());
                Utility.FileSorter(files);

                for (File f : files) {
                    String lcFileName = f.getName().toLowerCase();
                    if (!f.isHidden() && (lcFileName.endsWith(".qsp") || lcFileName.endsWith(".gam"))) {
                        gameFiles.add(f);
                    }
                }

                games.add(new Game(dir, gameFiles));
            }
        }

        return games;
    }

    private String getGameInfo(Game game) {
        File infoFile = new File(game.dir, GAME_INFO_FILENAME);
        if (infoFile.exists()) {
            try {
                return readFileAsString(infoFile);
            } catch (IOException e) {
                e.printStackTrace();
                Utility.WriteLog(context.getString(R.string.gameInfoFileReadError));
            }
        }

        return null;
    }

    private String readFileAsString(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        FileInputStream in = new FileInputStream(file);
        InputStreamReader inReader = new InputStreamReader(in);

        try (BufferedReader bufReader = new BufferedReader(inReader)) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
            }
        }

        return builder.toString();
    }

    private GameItem parseGameInfo(String xml) {
        GameItem resultItem = null;
        GameItem curItem = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            boolean docStarted = false;
            boolean gameStarted = false;
            String tagName = "";

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
                                curItem = new GameItem();
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (docStarted && gameStarted) {
                            if (xpp.getName().equals("game")) {
                                resultItem = curItem;
                            }
                            tagName = "";
                        }
                        break;
                    case XmlPullParser.CDSECT:
                        if (docStarted && gameStarted) {
                            fillGameItemFromCDATA(curItem, tagName, xpp.getText());
                        }
                        break;
                }
                eventType = xpp.nextToken();
            }
        } catch (XmlPullParserException e) {
            String text = context.getString(R.string.parseGameInfoXMLError)
                    .replace("-LINENUM-", String.valueOf(e.getLineNumber()))
                    .replace("-COLNUM-", String.valueOf(e.getColumnNumber()));

            Utility.WriteLog(text);
        } catch (Exception e) {
            Utility.WriteLog(context.getString(R.string.parseGameInfoUnkError));
        }

        return resultItem;
    }

    private void fillGameItemFromCDATA(GameItem item, String tagName, String value) {
        switch (tagName) {
            case "id":
                item.game_id = "id:".concat(value);
                break;
            case "list_id":
                item.list_id = value;
                break;
            case "author":
                item.author = value;
                break;
            case "ported_by":
                item.ported_by = value;
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
                item.file_url = value;
                break;
            case "file_size":
                item.file_size = Integer.parseInt(value);
                break;
            case "desc_url":
                item.desc_url = value;
                break;
            case "pub_date":
                item.pub_date = value;
                break;
            case "mod_date":
                item.mod_date = value;
                break;
        }
    }

    private static class Game {

        private final File dir;
        private final Collection<File> gameFiles;

        private Game(File dir, Collection<File> gameFiles) {
            this.dir = dir;
            this.gameFiles = gameFiles;
        }
    }
}
