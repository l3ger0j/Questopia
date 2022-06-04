package com.qsp.player.viewModel.repository;

import static com.qsp.player.utils.XmlUtil.xmlToObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.dto.stock.GameList;
import com.qsp.player.utils.StreamUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class RemoteGameRepository {
    private static final String GAMESTOCK_URL_V2 = "http://qsp.su/gamestock/gamestock2.php";

    private static final Logger logger = LoggerFactory.getLogger(RemoteGameRepository.class);

    private static List<GameData> cachedGameData = null;

    public List<GameData> getGames() {
        if (cachedGameData == null) {
            String xml = getGameStockXml();
            if (xml != null) {
                cachedGameData = parseGameStockXml(xml);
            }
        }
        return cachedGameData;
    }

    @Nullable
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

    @Nullable
    private List<GameData> parseGameStockXml(String xml) {
        try {
            GameList gameList = xmlToObject(xml, GameList.class);
            return filterSupported(gameList.gameDataList);
        } catch (Exception ex) {
            logger.error("Failed to parse game stock XML", ex);
            return null;
        }
    }

    @NonNull
    private List<GameData> filterSupported(List<GameData> gameData) {
        ArrayList<GameData> supported = new ArrayList<>();
        for (GameData data : gameData) {
            if (isSupportedFileType(data.fileExt)) {
                supported.add(data);
            } else {
                logger.warn("Skipping gameData '{}' because of unsupported file type '{}'", data.title, data.fileExt);
            }
        }
        return supported;
    }

    private boolean isSupportedFileType(String ext) {
        return ext.equals("zip") || ext.equals("rar") || ext.equals("aqsp");
    }
}
