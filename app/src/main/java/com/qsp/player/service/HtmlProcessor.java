package com.qsp.player.service;

import android.util.Base64;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.qsp.player.util.Base64Util.encodeBase64;
import static com.qsp.player.util.StringUtil.isNotEmpty;
import static com.qsp.player.util.StringUtil.isNullOrEmpty;

public class HtmlProcessor {
    private static final Logger logger = LoggerFactory.getLogger(HtmlProcessor.class);
    private static final Pattern execPattern = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);

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
        body.select("img").attr("style", "max-width: 100%;");
        body.select("video")
                .attr("style", "max-width: 100%;")
                .attr("muted", "true");

        return document.toString();
    }

    private String unescapeQuotes(String str) {
        return str.replace("\\\"", "'");
    }

    private String encodeExec(String html) {
        Matcher matcher = execPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String exec = normalizePathsInExec(matcher.group(1));
            String encodedExec = encodeBase64(exec, Base64.NO_WRAP);
            matcher.appendReplacement(sb, "href=\"exec:" + encodedExec + "\"");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String normalizePathsInExec(String exec) {
        return exec.replace("\\", "/");
    }

    private String htmlizeLineBreaks(String s) {
        return s.replace("\n", "<br>")
                .replace("\r", "");
    }

    /**
     * Привести строку <code>str</code>, полученную из библиотеки, к HTML-коду,
     * приемлемому для отобрабежния в {@linkplain android.webkit.WebView}.
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
