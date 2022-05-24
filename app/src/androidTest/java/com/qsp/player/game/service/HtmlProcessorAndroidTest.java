package com.qsp.player.game.service;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.model.service.HtmlProcessor;
import com.qsp.player.model.service.ImageProvider;

@RunWith(AndroidJUnit4.class)
public class HtmlProcessorAndroidTest {
    private final GameContentResolver gameContentResolver = new GameContentResolver();
    private final ImageProvider imageProvider = new ImageProvider();
    private final HtmlProcessor sut = new HtmlProcessor(gameContentResolver, imageProvider);

    @Test
    public void convertQspHtmlToWebViewHtml_execBlocks() {
        String html = "<a href=\"exec:'Test' & gt 'Test'\" />";

        String result = sut.convertQspHtmlToWebViewHtml(html);

        assertEquals("<html><head></head><body><a href=\"exec:J1Rlc3QnICYgZ3QgJ1Rlc3Qn\"></a></body></html>", result);
    }
}
