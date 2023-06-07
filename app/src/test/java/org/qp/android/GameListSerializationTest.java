package org.qp.android;

import static org.qp.android.utils.XmlUtil.xmlToObject;
import static org.junit.Assert.assertEquals;

import org.qp.android.dto.stock.InnerGameData;
import org.qp.android.dto.stock.GameList;
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
        expected.innerGameDataList = new ArrayList<>();
        expected.innerGameDataList.add(newGame("1"));
        expected.innerGameDataList.add(newGame("2"));
        GameList actual = xmlToObject(xml, GameList.class);
        assertEquals(expected, actual);
    }

    private InnerGameData newGame(String id) {
        InnerGameData innerGameData = new InnerGameData();
        innerGameData.id = id;
        return innerGameData;
    }
}
