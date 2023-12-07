package org.qp.android.model.service;

import static org.qp.android.helpers.utils.ThreadUtil.isMainThread;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class ImageProvider {
    private Bitmap tempBitmap;

    /**
     * Loads an image from a file using an absolute path.
     *
     * @return uploaded image, or <code>null</code> if the image was not found
     */
    public Drawable get(Uri path) {
        if (isMainThread()) {
            Picasso.get().load(path).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap , Picasso.LoadedFrom from) {
                    tempBitmap = bitmap;
                }

                @Override
                public void onBitmapFailed(Exception e , Drawable errorDrawable) {

                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });
        } else {
            new Handler(Looper.getMainLooper()).post(() ->
                    Picasso.get().load(path).into(new Target() {
                        @Override
                        public void onBitmapLoaded(Bitmap bitmap , Picasso.LoadedFrom from) {
                            tempBitmap = bitmap;
                        }

                        @Override
                        public void onBitmapFailed(Exception e , Drawable errorDrawable) {

                        }

                        @Override
                        public void onPrepareLoad(Drawable placeHolderDrawable) {
                        }
                    }));
        }
        return new BitmapDrawable(Resources.getSystem(), tempBitmap);
    }
}
