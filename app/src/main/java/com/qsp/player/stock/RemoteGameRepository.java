package com.qsp.player.stock;

import android.content.Context;

import com.qsp.player.R;
import com.qsp.player.Utility;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;

class RemoteGameRepository {

    private static Collection<GameItem> cachedGameItems = null;

    private final Context context;

    RemoteGameRepository(final Context context) {
        this.context = context;
    }

    Collection<GameItem> getGameItems() {
        if (cachedGameItems == null) {
            cachedGameItems = parseGameStockXml(getGameStockXml());
        }

        return cachedGameItems;
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
        } catch (Exception e) {
            Utility.WriteLog(context.getString(R.string.gamelistLoadExcept));
            return null;
        }
    }

    private Collection<GameItem> parseGameStockXml(String xml) {
        Collection<GameItem> items = new ArrayList<>();
        boolean parsed = false;
        if (xml != null) {
            GameItem curItem = null;
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);

                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(xml));

                boolean docStarted = false;
                boolean listStarted = false;
                String lastTagName = "";
                String listId = "unknown";

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_DOCUMENT:
                            docStarted = true;
                            break;
                        case XmlPullParser.START_TAG:
                            if (docStarted) {
                                lastTagName = xpp.getName();
                                if (lastTagName.equals("game_list")) {
                                    listStarted = true;
                                    listId = xpp.getAttributeValue(null, "id");
                                }
                                if (listStarted && lastTagName.equals("game")) {
                                    curItem = new GameItem();
                                    curItem.list_id = listId;
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if (docStarted && listStarted) {
                                if (xpp.getName().equals("game")) {
                                    items.add(curItem);
                                } else if (xpp.getName().equals("game_list")) {
                                    parsed = true;
                                }
                                lastTagName = "";
                            }
                            break;
                        case XmlPullParser.CDSECT:
                            if (docStarted && listStarted) {
                                fillGameItemFromCDATA(curItem, lastTagName, xpp.getText());
                            }
                            break;
                    }
                    eventType = xpp.nextToken();
                }
            } catch (XmlPullParserException e) {
                String errTxt = context.getString(R.string.parseGameInfoXMLError)
                        .replace("-LINENUM-", String.valueOf(e.getLineNumber()))
                        .replace("-COLNUM-", String.valueOf(e.getColumnNumber()));

                Utility.WriteLog(errTxt);
            } catch (Exception e) {
                Utility.WriteLog(context.getString(R.string.parseGameInfoUnkError));
            }
        }
        if (!parsed) {
            throw new GameListLoadException();
        }

        return items;
    }

    private void fillGameItemFromCDATA(GameItem item, String tagName, String value) {
        switch (tagName) {
            case "id":
                item.game_id = "id:".concat(value);
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
}
