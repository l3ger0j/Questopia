package com.qsp.player;

import static com.qsp.player.shared.util.XmlUtil.objectToXml;
import static com.qsp.player.shared.util.XmlUtil.xmlToObject;
import static org.junit.Assert.assertEquals;

import com.qsp.player.stock.dto.Game;

import org.junit.Test;

public class GameSerializationTest {

    @Test
    public void serializeToXml() throws Exception {
        Game game = new Game();
        game.id = "id";
        game.listId = "listId";
        game.author = "author";
        game.portedBy = "portedBy";
        game.version = "1.0";
        game.title = "title";
        game.lang = "ru";
        game.player = "player";
        game.fileUrl = "http://qsp.su/index2.php";
        game.fileSize = "1024";
        game.fileExt = "zip";
        game.descUrl = "http://qsp.su/index.php";
        game.pubDate = "2022-01-01 00:00:01";
        game.modDate = "2022-01-01 00:00:02";

        String actual = objectToXml(game);
        String expected = "" +
                "<game>" +
                "<id><![CDATA[" + game.id + "]]></id>" +
                "<list_id><![CDATA[" + game.listId + "]]></list_id>" +
                "<author><![CDATA[" + game.author + "]]></author>" +
                "<ported_by><![CDATA[" + game.portedBy + "]]></ported_by>" +
                "<version><![CDATA[" + game.version + "]]></version>" +
                "<title><![CDATA[" + game.title + "]]></title>" +
                "<lang><![CDATA[" + game.lang + "]]></lang>" +
                "<player><![CDATA[" + game.player + "]]></player>" +
                "<file_url><![CDATA[" + game.fileUrl + "]]></file_url>" +
                "<file_size><![CDATA[" + game.fileSize + "]]></file_size>" +
                "<file_ext><![CDATA[" + game.fileExt + "]]></file_ext>" +
                "<desc_url><![CDATA[" + game.descUrl + "]]></desc_url>" +
                "<pub_date><![CDATA[" + game.pubDate + "]]></pub_date>" +
                "<mod_date><![CDATA[" + game.modDate + "]]></mod_date>" +
                "</game>";

        assertEquals(expected, actual);
    }

    @Test
    public void deserializeFromXml() throws Exception {
        Game expected = new Game();
        expected.id = "id";
        expected.listId = "listId";
        expected.author = "author";
        expected.portedBy = "portedBy";
        expected.version = "1.0";
        expected.title = "title";
        expected.lang = "ru";
        expected.player = "player";
        expected.fileUrl = "http://qsp.su/index2.php";
        expected.fileSize = "1024";
        expected.fileExt = "zip";
        expected.descUrl = "http://qsp.su/index.php";
        expected.pubDate = "2022-01-01 00:00:01";
        expected.modDate = "2022-01-01 00:00:02";

        String xml = "" +
                "<game>" +
                "<id><![CDATA[" + expected.id + "]]></id>" +
                "<list_id><![CDATA[" + expected.listId + "]]></list_id>" +
                "<author><![CDATA[" + expected.author + "]]></author>" +
                "<ported_by><![CDATA[" + expected.portedBy + "]]></ported_by>" +
                "<version><![CDATA[" + expected.version + "]]></version>" +
                "<title><![CDATA[" + expected.title + "]]></title>" +
                "<lang><![CDATA[" + expected.lang + "]]></lang>" +
                "<player><![CDATA[" + expected.player + "]]></player>" +
                "<file_url><![CDATA[" + expected.fileUrl + "]]></file_url>" +
                "<file_size><![CDATA[" + expected.fileSize + "]]></file_size>" +
                "<file_ext><![CDATA[" + expected.fileExt + "]]></file_ext>" +
                "<desc_url><![CDATA[" + expected.descUrl + "]]></desc_url>" +
                "<pub_date><![CDATA[" + expected.pubDate + "]]></pub_date>" +
                "<mod_date><![CDATA[" + expected.modDate + "]]></mod_date>" +
                "</game>";

        Game actual = xmlToObject(xml, Game.class);
        assertEquals(expected, actual);
    }
}
