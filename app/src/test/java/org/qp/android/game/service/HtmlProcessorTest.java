package org.qp.android.game.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qp.android.helpers.service.HtmlProcessor;

public class HtmlProcessorTest {

    private final HtmlProcessor sut = new HtmlProcessor();

    @Test
    public void convertQspHtmlToWebViewHtml_escapedQuotes() {
        var html = "Test1 \\\"Test2\\\" Test3";

        var result = sut.getTestHtml(html);

        assertEquals("<html><head></head><body>Test1 'Test2' Test3</body></html>", result);
    }

    @Test
    public void convertQspHtmlToWebViewHtml_lineBreaks() {
        var html = "Test1\nTest2\r\nTest3";

        var result = sut.getTestHtml(html);

        assertEquals("<html><head></head><body>Test1<br>Test2<br>Test3</body></html>", result);
    }

    @Test
    public void convertQspStringToWebViewHtml_lineBreaks() {
        var str = "Test1\nTest2\r\nTest3";

        var result = sut.convertLibStrToHtml(str);

        assertEquals("Test1<br>Test2<br>Test3", result);
    }
}
