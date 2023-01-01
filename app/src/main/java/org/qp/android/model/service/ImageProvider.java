package org.qp.android.model.service;

import static org.qp.android.utils.StringUtil.isNullOrEmpty;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;

public class ImageProvider {
    private final String TAG = this.getClass().getSimpleName();
    private Drawable tempDrawable;

    /**
     * Loads an image from a file using an absolute path.
     *
     * @return uploaded image, or <code>null</code> if the image was not found
     */
    public Drawable get(String path) {
        if (isNullOrEmpty(path)) return null;
        var fixPath = path.replace("\\", "/").trim();
        var handler = new Handler(Looper.getMainLooper());
        handler.post(() ->
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
                }));
        return tempDrawable;
    }
}
