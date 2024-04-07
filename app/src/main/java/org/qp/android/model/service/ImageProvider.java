package org.qp.android.model.service;

import static org.qp.android.helpers.utils.ThreadUtil.isMainThread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class ImageProvider {

    private Bitmap mBitmap;

    /**
     * Loads an image from a file using an Uri.
     *
     * @return uploaded image, or <code>null</code> if the image was not found
     */
    public BitmapDrawable getDrawableFromPath(Context context , Uri path) {
        var target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap , Picasso.LoadedFrom from) {
                mBitmap = bitmap;
            }

            @Override
            public void onBitmapFailed(Exception e , Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        if (isMainThread()) {
            Picasso.get().load(path).into(target);
        } else {
            new Handler(Looper.getMainLooper()).post(() ->
                    Picasso.get().load(path).into(target));
        }
        return new BitmapDrawable(context.getResources() , mBitmap);
    }
}
