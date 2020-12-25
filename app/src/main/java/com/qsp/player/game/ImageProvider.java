package com.qsp.player.game;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

class ImageProvider implements ImageGetter {

    private static final String TAG = ImageProvider.class.getName();
    private static final HashMap<String, Drawable> CACHE = new HashMap<>();

    private final Context context;

    private File gameDir;

    ImageProvider(Context context) {
        this.context = context;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source == null || source.isEmpty()) return null;

        Drawable drawable = CACHE.get(source);
        if (drawable != null) {
            return drawable;
        }
        File file = new File(source);
        if (!file.exists()) {
            Log.e(TAG, "Image file not found: " + source);
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            drawable = Drawable.createFromStream(in, source);
        } catch (IOException e) {
            Log.e(TAG, "Failed reading from the image file", e);
            drawable = null;
        }
        if (drawable != null) {
            CACHE.put(file.getAbsolutePath(), drawable);
        }
        return drawable;
    }

    void setGameDirectory(File dir) {
        gameDir = dir;
        CACHE.clear();
    }
}
