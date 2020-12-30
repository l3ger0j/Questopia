package com.qsp.player.service;

import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import static com.qsp.player.util.StringUtil.isNullOrEmpty;

public class ImageProvider implements ImageGetter {
    private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);
    private static final HashMap<String, Drawable> cache = new HashMap<>();

    @Override
    public Drawable getDrawable(String source) {
        if (isNullOrEmpty(source)) return null;

        Drawable drawable = cache.get(source);
        if (drawable != null) return drawable;

        File file = new File(source);
        if (!file.exists()) {
            logger.error("Image file not found: " + source);
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            drawable = Drawable.createFromStream(in, source);
        } catch (IOException ex) {
            logger.error("Error reading the image file", ex);
        }
        if (drawable != null) {
            cache.put(file.getAbsolutePath(), drawable);
        }

        return drawable;
    }

    public void invalidateCache() {
        cache.clear();
    }
}
