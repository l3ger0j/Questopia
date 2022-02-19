package com.qsp.player.stock.repository;

import com.qsp.player.stock.GameStockItem;
import com.qsp.player.stock.GameStockItemBuilder;
import com.qsp.player.shared.util.StreamUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class RemoteGameRepository {
    private static final String GAMESTOCK_URL_V1 = "http://qsp.su/tools/gamestock/gamestock.php";
    private static final String GAMESTOCK_URL_V2 = "http://qsp.su/gamestock/gamestock2.php";

    private static final Logger logger = LoggerFactory.getLogger(RemoteGameRepository.class);

    private static List<GameStockItem> cachedGames = null;

    public List<GameStockItem> getGames() {
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
            URL url = new URL(GAMESTOCK_URL_V2);
            URLConnection conn = url.openConnection();
            try (InputStream in = conn.getInputStream()) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    StreamUtil.copy(in, out);
                    return out.toString();
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to fetch game stock XML", ex);
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
            GameStockItemBuilder itemBuilder = null;

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
                                itemBuilder = new GameStockItemBuilder();
                                itemBuilder.setListId(listId);
                            }
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (docStarted && listStarted) {
                            if (xpp.getName().equals("game")) {
                                GameStockItem item = itemBuilder.build();
                                if (item.fileExt.isEmpty() || item.fileExt.equals("zip") || item.fileExt.equals("rar")) {
                                    items.add(item);
                                } else {
                                    logger.warn("Unsupported file extension: " + item.fileExt);
                                }
                            }
                            tagName = "";
                        }
                        break;

                    case XmlPullParser.CDSECT:
                        if (docStarted && listStarted) {
                            itemBuilder.setFromXML(tagName, xpp.getText());
                        }
                        break;
                }
                eventType = xpp.nextToken();
            }

            return items;
        } catch (XmlPullParserException | IOException ex) {
            logger.error("Failed to parse game stock XML", ex);
            return null;
        }
    }
}
