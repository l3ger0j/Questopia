package com.qsp.player.util;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlUtil {

    private static final String TAG = HtmlUtil.class.getName();
    private static final Pattern EXEC_PATTERN = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);

    public static String preprocessQspHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String result = unescapeQuotes(html);
        result = encodeExec(result);
        result = htmlizeLineBreaks(result);

        Document document = Jsoup.parse(result);
        Element body = document.body();
        body.select("img").attr("style", "max-width: 100%;");

        // video setting
        body.select("video").attr("style", "max-width: 100%;");
        body.select("video").attr("muted", "true");

        return document.toString();
    }

    private static String unescapeQuotes(String str) {
        return str.replace("\\\"", "'");
    }

    private static String encodeExec(String html) {
        Matcher matcher = EXEC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String exec = normalizePathsInExec(matcher.group(1));
            String encodedExec = Base64.encodeToString(exec.getBytes(), Base64.NO_WRAP);
            matcher.appendReplacement(sb, "href=\"exec:" + encodedExec + "\"");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String normalizePathsInExec(String exec) {
        return exec.replace("\\", "/");
    }

    private static String htmlizeLineBreaks(String s) {
        return s.replace("\n", "<br>")
                .replace("\r", "");
    }

    public static String convertQspStringToHtml(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        return htmlizeLineBreaks(str);
    }

    public static String removeHtmlTags(@NonNull String html) {
        Objects.requireNonNull(html, "html must not be null");

        if (html.isEmpty()) {
            return html;
        }

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
                Log.w(TAG, String.format("Invalid HTML: element at %d is not closed", idx));
                result.append(html.substring(idx));
                break;
            }
            fromIdx = endIdx + 1;
        }

        return result.toString();
    }

    public static String decodeExec(String code) {
        return new String(Base64.decode(code, Base64.DEFAULT));
    }
}
