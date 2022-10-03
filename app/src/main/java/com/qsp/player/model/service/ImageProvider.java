package com.qsp.player.model.service;

import static com.qsp.player.utils.StringUtil.isNullOrEmpty;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;

public class ImageProvider {
    private final String TAG = this.getClass().getSimpleName();
    private Drawable tempDrawable;

    /**
     * Загружает изображение из файла по абсолютному пути или возвращает изображение из кеша.
     *
     * @return загруженное изображение, или <code>null</code> если изображение не было найдено
     */
    public Drawable get(String path) {
        if (isNullOrEmpty(path)) return null;
        String fixPath = path.replace("\\", "/").trim();
        Picasso.get()
                .load(new File(fixPath))
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap , Picasso.LoadedFrom from) {
                        tempDrawable = new BitmapDrawable(Resources.getSystem(), bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Exception e , Drawable errorDrawable) {
                        Log.e(TAG, "Error: ", e);
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }
                });
        return tempDrawable;
    }
}
