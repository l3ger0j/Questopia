package com.qsp.player.game;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

class ImageProvider implements ImageGetter {

    private static final String TAG = ImageProvider.class.getName();
    private static final HashMap<String, Drawable> CACHE = new HashMap<>();

    private final Context context;

    private DocumentFile gameDir;

    ImageProvider(Context context) {
        this.context = context;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        Drawable drawable = CACHE.get(source);
        if (drawable != null) {
            return drawable;
        }

        DocumentFile file = FileUtil.findFileByPath(gameDir, source);
        if (file == null) {
            Log.e(TAG, "Image file not found: " + source);
            return null;
        }

        try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
            drawable = Drawable.createFromStream(in, source);
        } catch (IOException e) {
            Log.e(TAG, "Failed reading from the image file", e);
            drawable = null;
        }
        if (drawable != null) {
            CACHE.put(file.getUri().toString(), drawable);
        }

        return drawable;
    }

    void setGameDirectory(DocumentFile dir) {
        gameDir = dir;
        CACHE.clear();
    }
}
