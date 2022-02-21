package com.qsp.player;

import static com.qsp.player.shared.util.XmlUtil.xmlToObject;
import static org.junit.Assert.assertEquals;

import com.qsp.player.stock.dto.Game;
import com.qsp.player.stock.dto.GameList;

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
        expected.gameList = new ArrayList<>();
        expected.gameList.add(newGame("1"));
        expected.gameList.add(newGame("2"));
        GameList actual = xmlToObject(xml, GameList.class);
        assertEquals(expected, actual);
    }

    private Game newGame(String id) {
        Game game = new Game();
        game.id = id;
        return game;
    }
}
