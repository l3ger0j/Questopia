package com.qsp.player.stock;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.util.FileUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.qsp.player.util.FileUtil.GAME_INFO_FILENAME;

class LocalGameRepository {

    private static final String TAG = LocalGameRepository.class.getName();

    private final Context context;

    private DocumentFile gamesDir;

    LocalGameRepository(Context context) {
        this.context = context;
    }

    void setGamesDirectory(DocumentFile dir) {
        gamesDir = dir;
    }

    List<GameStockItem> getGames() {
        if (gamesDir == null) {
            Log.e(TAG, "Games directory is not specified");
            return Collections.emptyList();
        }

        ArrayList<DocumentFile> gameDirs = getGameDirectories();
        if (gameDirs.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<GameStockItem> items = new ArrayList<>();
        for (GameFolder folder : getGameFolders(gameDirs)) {
            String info = getGameInfo(folder);
            boolean pack = folder.gameFiles.size() > 1;

            for (DocumentFile file : folder.gameFiles) {
                GameStockItem item;
                if (info != null) {
                    item = parseGameInfo(info);
                } else {
                    String name = folder.dir.getName();
                    item = new GameStockItem();
                    item.title = name;
                    item.gameId = name;
                }
                if (pack) {
                    String name = file.getName();
                    item.title += " (" + name + ")";
                    item.gameId += " (" + name + ")";
                }
                item.downloaded = true;
                item.localDirUri = folder.dir.getUri().toString();
                item.localFileUri = file.getUri().toString();
                items.add(item);
            }
        }

        return items;
    }

    private ArrayList<DocumentFile> getGameDirectories() {
        ArrayList<DocumentFile> dirs = new ArrayList<>();
        for (DocumentFile f : gamesDir.listFiles()) {
            if (f.isDirectory()) {
                dirs.add(f);
            }
        }

        return dirs;
    }

    private List<GameFolder> getGameFolders(List<DocumentFile> dirs) {
        ArrayList<GameFolder> folders = new ArrayList<>();
        sortFilesByName(dirs);

        for (DocumentFile dir : dirs) {
            ArrayList<DocumentFile> gameFiles = new ArrayList<>();

            List<DocumentFile> files = Arrays.asList(dir.listFiles());
            sortFilesByName(files);

            for (DocumentFile file : files) {
                String lcName = file.getName().toLowerCase();
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }

            folders.add(new GameFolder(dir, gameFiles));
        }

        return folders;
    }

    private void sortFilesByName(List<DocumentFile> files) {
        if (files.size() < 2) {
            return;
        }
        Collections.sort(files, new Comparator<DocumentFile>() {
            @Override
            public int compare(DocumentFile first, DocumentFile second) {
                return first.getName().toLowerCase()
                        .compareTo(second.getName().toLowerCase());
            }
        });
    }

    private String getGameInfo(GameFolder game) {
        DocumentFile infoFile = game.dir.findFile(GAME_INFO_FILENAME);
        if (infoFile == null) {
            Log.w(TAG, "Game info file not found in " + game.dir.getName());
            return null;
        }

        return FileUtil.readFileAsString(context, infoFile);
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
            Log.e(TAG, "Failed to parse game info file", e);
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
        private final DocumentFile dir;
        private final Collection<DocumentFile> gameFiles;

        private GameFolder(DocumentFile dir, Collection<DocumentFile> gameFiles) {
            this.dir = dir;
            this.gameFiles = gameFiles;
        }
    }
}
