package org.qp.android;

import static org.junit.Assert.assertEquals;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;

import org.junit.Test;
import org.qp.android.dto.stock.GameDataList;
import org.qp.android.dto.stock.RemoteGameData;

import java.util.ArrayList;

public class GameDataListSerializationTest {

    @Test
    public void deserializeFromJSON() throws Exception {
        var doneXml = """
                {
                  "gameDataList" : [ {
                    "author" : "",
                    "descUrl" : "",
                    "fileExt" : "",
                    "fileUrl" : "",
                    "icon" : "",
                    "id" : "1",
                    "lang" : "",
                    "modDate" : "",
                    "player" : "",
                    "portedBy" : "",
                    "pubDate" : "",
                    "title" : "",
                    "version" : ""
                  }, {
                    "author" : "",
                    "descUrl" : "",
                    "fileExt" : "",
                    "fileUrl" : "",
                    "icon" : "",
                    "id" : "2",
                    "lang" : "",
                    "modDate" : "",
                    "player" : "",
                    "portedBy" : "",
                    "pubDate" : "",
                    "title" : "",
                    "version" : ""
                  } ]
                }
                """;

        var expected = new GameDataList();
        expected.game = new ArrayList<>();
        expected.game.add(newGame("1"));
        expected.game.add(newGame("2"));
        var actual = jsonToObject(doneXml, GameDataList.class);
        assertEquals(expected, actual);
    }

    private RemoteGameData newGame(String id) {
        var gameData = new RemoteGameData();
        gameData.id = id;
        return gameData;
    }
}
