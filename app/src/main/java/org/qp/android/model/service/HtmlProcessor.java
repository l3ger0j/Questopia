package org.qp.android.model.service;

import static org.qp.android.helpers.utils.Base64Util.encodeBase64;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNullOrEmpty;

import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.qp.android.ui.settings.SettingsController;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

public class HtmlProcessor {

    private final String TAG = this.getClass().getSimpleName();

    private static final Pattern execPattern = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);
    private final ImageProvider imageProvider;
    private static final String HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>";
    private final Pattern pattern = Pattern.compile(HTML_PATTERN);
    private SettingsController controller;
    private DocumentFile curGameDir;

    @Nullable
    public String getSrcDir(String html) {
        var document = Jsoup.parse(html);
        var imageElement = document.select("img").first();
        if (imageElement == null) return null;
        return imageElement.attr("src");
    }

    public HtmlProcessor setController(SettingsController controller) {
        this.controller = controller;
        return this;
    }

    public void setCurGameDir(DocumentFile curGameDir) {
        this.curGameDir = curGameDir;
    }

    public boolean hasHTMLTags(String text){
        return pattern.matcher(text).find();
    }

    public HtmlProcessor(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    /**
     * Bring the HTML code <code>html</code> obtained from the library to
     * HTML code acceptable for display in {@linkplain android.webkit.WebView}.
     */
    public String convertQspHtmlToWebViewHtml(String html) {
        if (isNullOrEmpty(html)) return "";
        var result = unescapeQuotes(html);
        result = encodeExec(result);
        result = lineBreaksInHTML(result);
        var document = Jsoup.parse(result);
        document.outputSettings().prettyPrint(false);
        var body = document.body();
        processHTMLImages(body);
        processHTMLVideos(body);
        return document.toString();
    }

    public String convertLibHtmlToWebHtml(String html) {
        if (isNullOrEmpty(html)) return "";
        var result = unescapeQuotes(html);
        result = encodeExec(result);
        return lineBreaksInHTML(result);
    }

    public String getCleanHtmlPageAndImage(String dirtyHtml) {
        var document = Jsoup.parse(dirtyHtml);
        document.outputSettings().prettyPrint(false);
        var body = document.body();
        processHTMLImages(body);
        processHTMLVideos(body);
        return document.toString();
    }

    public String getCleanHtmlPageNotImage(String dirtyHtml) {
        var document = Jsoup.parse(dirtyHtml);
        document.outputSettings().prettyPrint(false);
        var body = document.body();
        body.select("img").remove();
        body.select("video").remove();
        return document.toString();
    }

    @NonNull
    private String unescapeQuotes(String str) {
        return str.replace("\\\"", "'");
    }

    @NonNull
    private String encodeExec(String html) {
        var matcher = execPattern.matcher(html);
        var sb = new StringBuffer();
        while (matcher.find()) {
            var exec = normalizePathsInExec(Objects.requireNonNull(matcher.group(1)));
            var encodedExec = encodeBase64(exec, Base64.NO_WRAP);
            matcher.appendReplacement(sb, "href=\"exec:" + encodedExec + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @NonNull
    private String normalizePathsInExec(@NonNull String exec) {
        return exec.replace("\\", "/");
    }

    @NonNull
    private String lineBreaksInHTML(@NonNull String s) {
        return s.replace("\n", "<br>")
                .replace("\r", "");
    }

    private void processHTMLImages(@NonNull Element documentBody) {
        var dynBlackList = new ArrayList<String>();
        documentBody.select("a").forEach(element -> {
            if (element.attr("href").contains("exec:")) {
                dynBlackList.add(element.select("img").attr("src"));
            }
        });

        documentBody.select("img").forEach(img -> {
            if (controller.isUseFullscreenImages) {
                if (!dynBlackList.contains(img.attr("src"))) {
                    img.attr("onclick" , "img.onClickImage(this.src);");
                }
            }
            if (controller.isUseAutoWidth && controller.isUseAutoHeight) {
                img.attr("style", "display: inline; height: auto; max-width: 100%;");
            }
            if (!controller.isUseAutoWidth) {
                if (shouldChangeWidth(img)) {
                    img.attr("style" , "max-width:" + controller.customWidthImage+";");
                }
            } else if (!controller.isUseAutoHeight) {
                if (shouldChangeHeight(img)) {
                    img.attr("style" , "max-height:" + controller.customHeightImage+";");
                }
            }
        });
    }

    private boolean shouldChangeWidth(Element img) {
        var relPath = img.attr("src");
        var imageFile = fromRelPath(relPath , curGameDir);
        if (imageFile == null) return false;
        var drawable = imageProvider.getDrawableFromPath(imageFile.getUri());
        if (drawable == null) return false;
        return drawable.getIntrinsicWidth() < Resources.getSystem()
                .getDisplayMetrics().widthPixels;
    }

    private boolean shouldChangeHeight(Element img) {
        var relPath = img.attr("src");
        var imageFile = fromRelPath(relPath , curGameDir);
        if (imageFile == null) return false;
        var drawable = imageProvider.getDrawableFromPath(imageFile.getUri());
        if (drawable == null) return false;
        return drawable.getIntrinsicHeight() < Resources.getSystem()
                .getDisplayMetrics().heightPixels;
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
        return isNotEmpty(str) ? lineBreaksInHTML(str) : "";
    }

    /**
     * Remove HTML tags from the <code>html</code> string and return the resulting string.
     */
    public String removeHTMLTags(String html) {
        if (isNullOrEmpty(html)) return "";
        var result = new StringBuilder();
        var len = html.length();
        var fromIdx = 0;
        while (fromIdx < len) {
            var idx = html.indexOf('<', fromIdx);
            if (idx == -1) {
                result.append(html.substring(fromIdx));
                break;
            }
            result.append(html, fromIdx, idx);
            var endIdx = html.indexOf('>', idx + 1);
            if (endIdx == -1) {
                var message = "Invalid HTML:"
                        + "\n source HTML:" + html
                        + "\n element at " + idx
                        + " is not closed";
                Log.w(TAG , message);
                result.append(html.substring(idx));
                break;
            }
            fromIdx = endIdx + 1;
        }
        return result.toString();
    }

}
