package com.qsp.player.stock;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

class RemoteGameRepository {

    private static final String TAG = RemoteGameRepository.class.getName();

    private static List<GameStockItem> cachedGames = null;

    List<GameStockItem> getGames() {
        if (cachedGames == null) {
            String xml = getGameStockXml();
            if (xml != null) {
                cachedGames = parseGameStockXml(xml);
            }
        }

        return cachedGames;
    }

    private String getGameStockXml() {
        try {
            URL url = new URL("http://qsp.su/tools/gamestock/gamestock.php");
            URLConnection conn = url.openConnection();
            byte[] b = new byte[8192];
            try (InputStream in = conn.getInputStream()) {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    int bytesRead;
                    while ((bytesRead = in.read(b)) > 0) {
                        os.write(b, 0, bytesRead);
                    }
                    return new String(os.toByteArray());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch game stock XML", e);
            return null;
        }
    }

    private ArrayList<GameStockItem> parseGameStockXml(String xml) {
        try {
            ArrayList<GameStockItem> items = new ArrayList<>();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            boolean docStarted = false;
            String tagName = "";
            boolean listStarted = false;
            String listId = "unknown";
            GameStockItem item = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        docStarted = true;
                        break;

                    case XmlPullParser.START_TAG:
                        if (docStarted) {
                            tagName = xpp.getName();
                            if (tagName.equals("game_list")) {
                                listStarted = true;
                                listId = xpp.getAttributeValue(null, "id");
                            }
                            if (listStarted && tagName.equals("game")) {
                                item = new GameStockItem();
                                item.listId = listId;
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (docStarted && listStarted) {
                            if (xpp.getName().equals("game")) {
                                items.add(item);
                            }
                            tagName = "";
                        }
                        break;

                    case XmlPullParser.CDSECT:
                        if (docStarted && listStarted) {
                            fillGameItemFromCDATA(item, tagName, xpp.getText());
                        }
                        break;
                }
                eventType = xpp.nextToken();
            }

            return items;
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Failed to parse game stock XML", e);
            return null;
        }
    }

    private void fillGameItemFromCDATA(GameStockItem item, String tagName, String value) {
        switch (tagName) {
            case "id":
                item.gameId = "id:".concat(value);
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
}
