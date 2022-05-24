package com.qsp.player.game.service;

import static org.junit.Assert.assertEquals;

import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.model.service.HtmlProcessor;
import com.qsp.player.model.service.ImageProvider;

import org.junit.Test;

public class HtmlProcessorTest {
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor sut = new HtmlProcessor(gameContentResolver, imageProvider);

    @Test
    public void convertQspHtmlToWebViewHtml_escapedQuotes() {
        String html = "Test1 \\\"Test2\\\" Test3";

        String result = sut.convertQspHtmlToWebViewHtml(html);

        assertEquals("<html><head></head><body>Test1 'Test2' Test3</body></html>", result);
    }

    @Test
    public void convertQspHtmlToWebViewHtml_lineBreaks() {
        String html = "Test1\nTest2\r\nTest3";

        String result = sut.convertQspHtmlToWebViewHtml(html);

        assertEquals("<html><head></head><body>Test1<br>Test2<br>Test3</body></html>", result);
    }

    @Test
    public void convertQspStringToWebViewHtml_lineBreaks() {
        String str = "Test1\nTest2\r\nTest3";

        String result = sut.convertQspStringToWebViewHtml(str);

        assertEquals("Test1<br>Test2<br>Test3", result);
    }
}
