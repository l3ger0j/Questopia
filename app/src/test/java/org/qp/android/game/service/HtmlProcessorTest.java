package org.qp.android.game.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

public class HtmlProcessorTest {

    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor sut = new HtmlProcessor(imageProvider);

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
