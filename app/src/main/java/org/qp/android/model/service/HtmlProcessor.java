package org.qp.android.model.service;

import static org.qp.android.helpers.utils.Base64Util.encodeBase64;
import static org.qp.android.helpers.utils.FileUtil.findFileFromRelPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNullOrEmpty;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
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

    public String getSrcDir(String html) {
        var document = Jsoup.parse(html);
        var imageElement = document.select("img").first();
        if (imageElement == null) return "";
        return imageElement.attr("src");
    }

    /**
     * Bring the HTML code <code>html</code> obtained from the library to
     * HTML code acceptable for display in {@linkplain android.webkit.WebView}.
     */
    public String getCleanHtmlPageAndImage(@NonNull Context context ,
                                           @NonNull String dirtyHtml) {
        var document = Jsoup.parse(dirtyHtml);
        document.outputSettings().prettyPrint(false);
        var body = document.body();
        processHTMLImages(context , body);
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

    public HtmlProcessor setController(SettingsController controller) {
        this.controller = controller;
        return this;
    }

    public HtmlProcessor setCurGameDir(DocumentFile curGameDir) {
        this.curGameDir = curGameDir;
        return this;
    }

    public boolean hasHTMLTags(String text){
        return pattern.matcher(text).find();
    }

    public HtmlProcessor(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    public String convertLibHtmlToWebHtml(String html) {
        if (isNullOrEmpty(html)) return "";
        var result = unescapeQuotes(html);
        result = encodeExec(result);
        return lineBreaksInHTML(result);
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

    private void processHTMLImages(@NonNull Context context,
                                   @NonNull Element documentBody) {
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
                if (shouldChangeWidth(context , img)) {
                    img.attr("style" , "max-width:" + controller.customWidthImage+";");
                }
            } else if (!controller.isUseAutoHeight) {
                if (shouldChangeHeight(context , img)) {
                    img.attr("style" , "max-height:" + controller.customHeightImage+";");
                }
            }
        });
    }

    private boolean shouldChangeWidth(Context context,
                                      Element img) {
        var relPath = img.attr("src");
        var imageFile = findFileFromRelPath(context , relPath , curGameDir);
        if (imageFile == null) return false;
        var drawable = imageProvider.getDrawableFromPath(context , imageFile.getUri());
        if (drawable == null) return false;
        var widthPix = context.getResources().getDisplayMetrics().widthPixels;
        return drawable.getIntrinsicWidth() < widthPix;
    }

    private boolean shouldChangeHeight(Context context,
                                       Element img) {
        var relPath = img.attr("src");
        var imageFile = findFileFromRelPath(context , relPath , curGameDir);
        if (imageFile == null) return false;
        var drawable = imageProvider.getDrawableFromPath(context , imageFile.getUri());
        if (drawable == null) return false;
        var heightPix = context.getResources().getDisplayMetrics().heightPixels;
        return drawable.getIntrinsicHeight() < heightPix;
    }

    private void processHTMLVideos(Element documentBody) {
        var videoElement = documentBody.select("video");
        videoElement.attr("style", "max-width:100%;");
        if (controller.isVideoMute) {
            videoElement.attr("muted", "true");
            videoElement.removeAttr("controls");
        } else {
            videoElement.attr("controls", "true");
            videoElement.removeAttr("muted");
        }
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
                Log.w(TAG,"Invalid HTML: element at " + idx + " is not closed");
                result.append(html.substring(idx));
                break;
            }
            fromIdx = endIdx + 1;
        }
        return result.toString();
    }

    public String removeHTMLTagsAsIs(String dirtyHTML) {
        if (isNullOrEmpty(dirtyHTML)) return "";
        return Jsoup.clean(dirtyHTML , Safelist.none());
    }

}
