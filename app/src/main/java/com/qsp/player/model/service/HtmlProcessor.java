package com.qsp.player.model.service;

import android.graphics.drawable.Drawable;
import android.util.Base64;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// FIXME: 24.05.2022
import static com.qsp.player.utils.Base64Util.encodeBase64;
import static com.qsp.player.utils.StringUtil.isNotEmpty;
import static com.qsp.player.utils.StringUtil.isNullOrEmpty;

import androidx.annotation.NonNull;

public class HtmlProcessor {
    private static final int IMAGE_WIDTH_THRESHOLD = 400;

    private static final Logger logger = LoggerFactory.getLogger(HtmlProcessor.class);
    private static final Pattern execPattern = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);

    private final GameContentResolver gameContentResolver;
    private final ImageProvider imageProvider;

    public HtmlProcessor(GameContentResolver gameContentResolver, ImageProvider imageProvider) {
        this.gameContentResolver = gameContentResolver;
        this.imageProvider = imageProvider;
    }

    /**
     * Привести HTML-код <code>html</code>, полученный из библиотеки к
     * HTML-коду, приемлемому для отображения в {@linkplain android.webkit.WebView}.
     */
    public String convertQspHtmlToWebViewHtml(String html) {
        if (isNullOrEmpty(html)) return "";

        String result = unescapeQuotes(html);
        result = encodeExec(result);
        result = htmlizeLineBreaks(result);

        Document document = Jsoup.parse(result);
        document.outputSettings().prettyPrint(false);

        Element body = document.body();
        processHtmlImages(body);
        processHtmlVideos(body);

        return document.toString();
    }

    @NonNull
    private String unescapeQuotes(String str) {
        return str.replace("\\\"", "'");
    }

    @NonNull
    private String encodeExec(String html) {
        Matcher matcher = execPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String exec = normalizePathsInExec(Objects.requireNonNull(matcher.group(1)));
            String encodedExec = encodeBase64(exec, Base64.NO_WRAP);
            matcher.appendReplacement(sb, "href=\"exec:" + encodedExec + "\"");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    @NonNull
    private String normalizePathsInExec(String exec) {
        return exec.replace("\\", "/");
    }

    @NonNull
    private String htmlizeLineBreaks(String s) {
        return s.replace("\n", "<br>")
                .replace("\r", "");
    }

    private void processHtmlImages(Element documentBody) {
        for (Element img : documentBody.select("img")) {
            boolean resize = shouldImageBeResized(img);
            if (resize) {
                img.attr("style", "max-width:100%;");
            }
        }
    }

    private boolean shouldImageBeResized(Element img) {
        String relPath = img.attr("src");
        String absPath = gameContentResolver.getAbsolutePath(relPath);

        Drawable drawable = imageProvider.get(absPath);
        if (drawable == null) return false;

        return drawable.getIntrinsicWidth() > IMAGE_WIDTH_THRESHOLD;
    }

    private void processHtmlVideos(Element documentBody) {
        documentBody.select("video")
                .attr("style", "max-width:100%;")
                .attr("muted", "true");
    }

    /**
     * Привести строку <code>str</code>, полученную из библиотеки, к HTML-коду,
     * приемлемому для отображения в {@linkplain android.webkit.WebView}.
     */
    public String convertQspStringToWebViewHtml(String str) {
        return isNotEmpty(str) ? htmlizeLineBreaks(str) : "";
    }

    /**
     * Удалить HTML-теги из строки <code>html</code> и вернуть результирующую строку.
     */
    public String removeHtmlTags(String html) {
        if (isNullOrEmpty(html)) return "";

        StringBuilder result = new StringBuilder();

        int len = html.length();
        int fromIdx = 0;
        while (fromIdx < len) {
            int idx = html.indexOf('<', fromIdx);
            if (idx == -1) {
                result.append(html.substring(fromIdx));
                break;
            }
            result.append(html, fromIdx, idx);
            int endIdx = html.indexOf('>', idx + 1);
            if (endIdx == -1) {
                logger.warn("Invalid HTML: element at {} is not closed", idx);
                result.append(html.substring(idx));
                break;
            }
            fromIdx = endIdx + 1;
        }

        return result.toString();
    }
}
