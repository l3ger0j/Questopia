package org.qp.android.model.service;

import static org.qp.android.utils.Base64Util.encodeBase64;
import static org.qp.android.utils.StringUtil.isNotEmpty;
import static org.qp.android.utils.StringUtil.isNullOrEmpty;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlProcessor {
    private final String TAG = this.getClass().getSimpleName();
    private static final int IMAGE_WIDTH_THRESHOLD = 400;
    private static final Pattern execPattern = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);
    private final GameContentResolver gameContentResolver;
    private final ImageProvider imageProvider;
    private static final String HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";
    private final Pattern pattern = Pattern.compile(HTML_PATTERN);

    public ObservableBoolean useOldValue = new ObservableBoolean();

    public HtmlProcessor(GameContentResolver gameContentResolver, ImageProvider imageProvider) {
        this.gameContentResolver = gameContentResolver;
        this.imageProvider = imageProvider;
    }

    /**
     * Bring the HTML code <code>html</code> obtained from the library to
     * HTML code acceptable for display in {@linkplain android.webkit.WebView}.
     */
    public String convertQspHtmlToWebViewHtml(String html) {
        if (isNullOrEmpty(html)) return "";

        String result = unescapeQuotes(html);
        result = encodeExec(result);
        result = htmlizeLineBreaks(result);

        Document document = Jsoup.parse(result);
        document.outputSettings().prettyPrint(false);

        Element body = document.body();
        processHTMLImages(body);
        processHTMLVideos(body);

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

    private void processHTMLImages(Element documentBody) {
        for (Element img : documentBody.select("img")) {
            if (shouldImageBeResized(img)) {
                img.attr("style", "max-width:100%;");
            }
        }
    }

    private boolean shouldImageBeResized(Element img) {
        String relPath = img.attr("src");
        String absPath = gameContentResolver.getAbsolutePath(relPath);

        Drawable drawable;
        drawable = imageProvider.get(absPath);
        if (drawable == null) return false;

        if (useOldValue.get()) {
            return drawable.getIntrinsicWidth() > IMAGE_WIDTH_THRESHOLD;
        } else {
            return drawable.getIntrinsicWidth() > Resources.getSystem()
                    .getDisplayMetrics().widthPixels;
        }
    }

    private void processHTMLVideos(Element documentBody) {
        documentBody.select("video")
                .attr("style", "max-width:100%;")
                .attr("muted", "true");
    }

    /**
     * Convert the string <code>str</code> obtained from the library to HTML code,
     * acceptable for display in {@linkplain android.webkit.WebView}.
     */
    public String convertQspStringToWebViewHtml(String str) {
        return isNotEmpty(str) ? htmlizeLineBreaks(str) : "";
    }

    /**
     * Remove HTML tags from the <code>html</code> string and return the resulting string.
     */
    public String removeHTMLTags(String html) {
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
                Log.w(TAG,"Invalid HTML: element at " + idx + " is not closed");
                result.append(html.substring(idx));
                break;
            }
            fromIdx = endIdx + 1;
        }

        return result.toString();
    }

    public boolean hasHTMLTags(String text){
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }
}
