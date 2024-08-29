package org.qp.android.model.service;

import static org.qp.android.helpers.utils.Base64Util.encodeBase64;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNullOrEmpty;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.qp.android.ui.settings.SettingsController;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class HtmlProcessor {

    private final String TAG = this.getClass().getSimpleName();

    private static final Pattern EXEC_PATTERN = Pattern.compile("href=\"exec:([\\s\\S]*?)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_PATTERN = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>");
    private static final Pattern BODY_PATTERN = Pattern.compile(".*?<body.*?>(.*?)</body>.*?", Pattern.DOTALL);

    private final ExecutorService executors = Executors.newSingleThreadExecutor();

    private final ImageProvider imageProvider;
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
    public String getCleanHtmlAndMedia(@NonNull Context context ,
                                       @NonNull String dirtyHtml) {
        if (isNullOrEmpty(dirtyHtml)) return "";

        var document = Jsoup.parse(preHandleHtml(dirtyHtml));
        document.outputSettings().prettyPrint(true);
        var body = document.body();
        handleImagesInHtml(context , body);
        handleVideosInHtml(body);

        return document.toString();
    }

    public String getCleanHtmlRemMedia(String dirtyHtml) {
        if (isNullOrEmpty(dirtyHtml)) return "";

        var document = Jsoup.parse(preHandleHtml(dirtyHtml));
        document.outputSettings().prettyPrint(false);
        var body = document.body();
        body.select("img").remove();
        body.select("video").remove();

        return document.toString();
    }

    public String getTestHtml(String dirtyHtml) {
        if (isNullOrEmpty(dirtyHtml)) return "";

        var document = Jsoup.parse(convertLibHtmlToWebHtml(dirtyHtml));
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

    public boolean isContainsHtmlTags(String text){
        return HTML_PATTERN.matcher(text).find();
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

    /**
     * Convert the string <code>str</code> obtained from the library to HTML code,
     * acceptable for display in {@linkplain android.webkit.WebView}.
     */
    public String convertLibStrToHtml(String str) {
        return isNotEmpty(str) ? lineBreaksInHTML(str) : "";
    }

    /**
     * Remove HTML tags from the <code>html</code> string and return the resulting string.
     */
    public String removeHtmlTags(String html) {
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
                return Jsoup.clean(html , Safelist.none());
            }
            fromIdx = endIdx + 1;
        }

        return result.toString();
    }

    @NonNull
    private String unescapeQuotes(String str) {
        return str.replace("\\\"", "'");
    }

    @NonNull
    private String encodeExec(String html) {
        var matcher = EXEC_PATTERN.matcher(html);
        var buffer = new StringBuffer();
        while (matcher.find()) {
            if (matcher.group(1) == null) continue;
            var exec = normalizePathsInExec(matcher.group(1));
            var encodedExec = encodeBase64(exec, Base64.NO_WRAP);
            matcher.appendReplacement(buffer, "href=\"exec:" + encodedExec + "\"");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
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

    private void handleImagesInHtml(@NonNull Context context,
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
                shouldChangeWidth(context, img).thenAccept(aBoolean -> {
                    if (!aBoolean) return;
                    img.attr("style" , "max-width:" + controller.customWidthImage+";");
                });
            } else if (!controller.isUseAutoHeight) {
                shouldChangeHeight(context, img).thenAccept(aBoolean -> {
                   if (!aBoolean) return;
                   img.attr("style" , "max-height:" + controller.customHeightImage+";");
                });
            }
        });
    }

    private CompletableFuture<Boolean> shouldChangeWidth(Context context,
                                                         Element img) {
        var relPath = img.attr("src");
        return CompletableFuture
                .supplyAsync(() -> fromRelPath(context , relPath , curGameDir), executors)
                .thenApply(imageFile -> {
                    if (imageFile == null) return false;
                    var drawable = imageProvider.getDrawableFromPath(context , imageFile.getUri());
                    if (drawable == null) return false;
                    var widthPix = context.getResources().getDisplayMetrics().widthPixels;
                    return drawable.getIntrinsicWidth() < widthPix;
                });
    }

    private CompletableFuture<Boolean> shouldChangeHeight(Context context,
                                                          Element img) {
        var relPath = img.attr("src");
        return CompletableFuture
                .supplyAsync(() -> fromRelPath(context , relPath , curGameDir), executors)
                .thenApply(imageFile -> {
                    if (imageFile == null) return false;
                    var drawable = imageProvider.getDrawableFromPath(context , imageFile.getUri());
                    if (drawable == null) return false;
                    var heightPix = context.getResources().getDisplayMetrics().heightPixels;
                    return drawable.getIntrinsicHeight() < heightPix;
                });
    }

    private void handleVideosInHtml(Element documentBody) {
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

    private String preHandleHtml(String dirtyHtml) {
        var bodyDirt = extractBody(dirtyHtml);

        if (bodyDirt.contains("\\\"")) {
            unescapeQuotes(bodyDirt);
        }
        if (EXEC_PATTERN.matcher(bodyDirt).find()) {
            encodeExec(bodyDirt);
        }
        if (bodyDirt.contains("\n") || bodyDirt.contains("\r")) {
            lineBreaksInHTML(bodyDirt);
        }

        var headDirt = dirtyHtml.split(".*?<body.*?>(.*?)</body>.*?")[0];
        return headDirt+"<body>"+bodyDirt+"</body>";
    }

    private String extractBody(String html) {
        var match = BODY_PATTERN.matcher(html);
        while (match.find()) {
            if (match.group(1) == null) continue;
            return match.group(1);
        }
        return "";
    }

}
