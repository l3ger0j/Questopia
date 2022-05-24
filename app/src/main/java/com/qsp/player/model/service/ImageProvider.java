package com.qsp.player.model.service;

import android.graphics.drawable.Drawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
// FIXME: 24.05.2022
import static com.qsp.player.utils.StringUtil.isNullOrEmpty;

public class ImageProvider {
    private static final Logger logger = LoggerFactory.getLogger(ImageProvider.class);
    private static final HashMap<String, Drawable> cache = new HashMap<>();

    /**
     * Загружает изображение из файла по абсолютному пути или возвращает изображение из кеша.
     *
     * @return загруженное изображение, или <code>null</code> если изображение не было найдено
     */
    public Drawable get(String path) {
        if (isNullOrEmpty(path)) return null;
        String normPath = path.replace("\\", "/");

        Drawable drawable = cache.get(normPath);
        if (drawable != null) return drawable;

        File file = new File(normPath);
        if (!file.exists()) {
            logger.error("Image file not found: " + normPath);
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            drawable = Drawable.createFromStream(in, normPath);
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
