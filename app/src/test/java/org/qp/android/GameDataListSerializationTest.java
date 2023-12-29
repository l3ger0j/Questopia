package org.qp.android;

import static org.junit.Assert.assertEquals;
import static org.qp.android.helpers.utils.XmlUtil.xmlToObject;

import org.junit.Test;
import org.qp.android.dto.stock.GameData;
import org.qp.android.dto.stock.GameDataList;

import java.util.ArrayList;

public class GameDataListSerializationTest {

    @Test
    public void deserializeFromXml() throws Exception {
        var doneXml = """
                <game_list><gameDataList><gameDataList><author></author><descUrl></descUrl><fileExt></fileExt><fileUrl></fileUrl><icon></icon><id>1</id><lang></lang><modDate></modDate><player></player><portedBy></portedBy><pubDate></pubDate><title></title><version></version></gameDataList><gameDataList><author></author><descUrl></descUrl><fileExt></fileExt><fileUrl></fileUrl><icon></icon><id>2</id><lang></lang><modDate></modDate><player></player><portedBy></portedBy><pubDate></pubDate><title></title><version></version></gameDataList></gameDataList></game_list>
                """;

        var expected = new GameDataList();
        expected.gameDataList = new ArrayList<>();
        expected.gameDataList.add(newGame("1"));
        expected.gameDataList.add(newGame("2"));
        var actual = xmlToObject(doneXml, GameDataList.class);
        assertEquals(expected, actual);
    }

    private GameData newGame(String id) {
        var gameData = new GameData();
        gameData.id = id;
        return gameData;
    }
}
