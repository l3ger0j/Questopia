package com.qsp.player;

import static com.qsp.player.utils.XmlUtil.xmlToObject;
import static org.junit.Assert.assertEquals;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.dto.stock.GameList;
import org.junit.Test;
import java.util.ArrayList;

public class GameListSerializationTest {

    @Test
    public void deserializeFromXml() throws Exception {
        String xml = "" +
                "<game_list>" +
                "<game>" +
                "<id><![CDATA[1]]></id>" +
                "</game>" +
                "<game>" +
                "<id><![CDATA[2]]></id>" +
                "</game>" +
                "</game_list>";

        GameList expected = new GameList();
        expected.gameDataList = new ArrayList<>();
        expected.gameDataList.add(newGame("1"));
        expected.gameDataList.add(newGame("2"));
        GameList actual = xmlToObject(xml, GameList.class);
        assertEquals(expected, actual);
    }

    private GameData newGame(String id) {
        GameData gameData = new GameData();
        gameData.id = id;
        return gameData;
    }
}
