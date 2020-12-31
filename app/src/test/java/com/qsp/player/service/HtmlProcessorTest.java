package com.qsp.player.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlProcessorTest {
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor sut = new HtmlProcessor(gameContentResolver, imageProvider);

    @Test
    public void convertQspHtmlToWebViewHtml_escapedQuotes() {
        String html = "Test1 \\\"Test2\\\" Test3";

        String result = sut.convertQspHtmlToWebViewHtml(html, 0);

        assertEquals("<html><head></head><body>Test1 'Test2' Test3</body></html>", result);
    }

    @Test
    public void convertQspHtmlToWebViewHtml_lineBreaks() {
        String html = "Test1\nTest2\r\nTest3";

        String result = sut.convertQspHtmlToWebViewHtml(html, 0);

        assertEquals("<html><head></head><body>Test1<br>Test2<br>Test3</body></html>", result);
    }

    @Test
    public void convertQspHtmlToWebViewHtml_imageStyle() {
        String html = "<img src=\"test.jpg\">";

        String result = sut.convertQspHtmlToWebViewHtml(html, 0);

        assertEquals("<html><head></head><body><img src=\"test.jpg\" style=\"max-width: 100%;\"></body></html>", result);
    }

    @Test
    public void convertQspStringToWebViewHtml_lineBreaks() {
        String str = "Test1\nTest2\r\nTest3";

        String result = sut.convertQspStringToWebViewHtml(str);

        assertEquals("Test1<br>Test2<br>Test3", result);
    }
}
