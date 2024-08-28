package org.qp.android.game.service;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;

@RunWith(AndroidJUnit4.class)
public class HtmlProcessorAndroidTest {

    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor sut = new HtmlProcessor(imageProvider);

    @Test
    public void convertQspHtmlToWebViewHtml_execBlocks() {
        String html = "<a href=\"exec:'Test' & gt 'Test'\" />";

        String result = sut.getTestHtml(html);

        assertEquals("<html><head></head><body><a href=\"exec:J1Rlc3QnICYgZ3QgJ1Rlc3Qn\"></a></body></html>", result);
    }
}
