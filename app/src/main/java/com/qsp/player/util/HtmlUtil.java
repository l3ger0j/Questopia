package com.qsp.player.util;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;

public final class HtmlUtil {

    private static final String TAG = HtmlUtil.class.getName();

    public static String preprocessQspHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        String result = encodeExec(html);
        result = htmlizeLineBreaks(result);

        Document document = Jsoup.parse(result);
        Element body = document.body();
        body.select("img").attr("style", "max-width: 100%;");

        return document.toString();
    }

    private static String encodeExec(String html) {
        StringBuilder result = new StringBuilder();
        int len = html.length();

        int fromIdx = 0;
        while (fromIdx < len) {
            int idx = indexOfIgnoreCase(html, "<a ", fromIdx);
            if (idx == -1) {
                result.append(html.substring(fromIdx));
                break;
            }
            result.append(html, fromIdx, idx + 3);
            fromIdx = idx + 3;

            int anchorEndIdx = html.indexOf('>', fromIdx);
            if (anchorEndIdx == -1) {
                Log.w(TAG, String.format("Invalid HTML: anchor element at %d is not closed", idx));
                result.append(html.substring(fromIdx));
                break;
            }

            idx = indexOfIgnoreCase(html, "href=\"exec:", fromIdx, anchorEndIdx);
            if (idx == -1) {
                result.append(html, fromIdx, anchorEndIdx + 1);
                fromIdx = anchorEndIdx + 1;
                continue;
            }

            result.append(html, fromIdx, idx + 11);
            fromIdx = idx + 11;

            int hrefEndIdx = indexOfIgnoreCase(html, "\"", fromIdx, anchorEndIdx);
            if (hrefEndIdx == -1) {
                Log.w(TAG, String.format("Invalid HTML: href attribute at %d is not closed", idx));
                result.append(html.substring(fromIdx));
                break;
            }

            String code = html.substring(fromIdx, hrefEndIdx);
            String codeB64 = Base64.encodeToString(code.getBytes(), Base64.NO_WRAP);
            result.append(codeB64);
            result.append(html, hrefEndIdx, anchorEndIdx + 1);
            fromIdx = anchorEndIdx + 1;
        }

        return result.toString();
    }

    private static String htmlizeLineBreaks(String s) {
        return s.replace("\n", "<br>")
                .replace("\r", "");
    }

    private static int indexOfIgnoreCase(String str, String substr, int fromIndex) {
        return str.toLowerCase().indexOf(substr, fromIndex);
    }

    private static int indexOfIgnoreCase(String str, String substr, int fromIndex, int endIndex) {
        return str.substring(0, endIndex).toLowerCase().indexOf(substr, fromIndex);
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
