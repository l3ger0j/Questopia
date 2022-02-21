package com.qsp.player.stock.repository;

import static com.qsp.player.shared.util.XmlUtil.xmlToObject;

import com.qsp.player.shared.util.StreamUtil;
import com.qsp.player.stock.dto.Game;
import com.qsp.player.stock.dto.GameList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class RemoteGameRepository {
    private static final String GAMESTOCK_URL_V1 = "http://qsp.su/tools/gamestock/gamestock.php";
    private static final String GAMESTOCK_URL_V2 = "http://qsp.su/gamestock/gamestock2.php";

    private static final Logger logger = LoggerFactory.getLogger(RemoteGameRepository.class);

    private static List<Game> cachedGames = null;

    public List<Game> getGames() {
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

    private List<Game> parseGameStockXml(String xml) {
        try {
            GameList gameList = xmlToObject(xml, GameList.class);
            return gameList.gameList;
        } catch (Exception ex) {
            logger.error("Failed to parse game stock XML", ex);
            return null;
        }
    }
}
