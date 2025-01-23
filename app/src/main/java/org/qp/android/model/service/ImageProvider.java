package org.qp.android.model.service;

import static org.qp.android.helpers.utils.ThreadUtil.isMainThread;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ImageProvider {

    private Bitmap mBitmap;

    /**
     * Loads an image from a file using an Uri.
     *
     * @return uploaded image, or <code>null</code> if the image was not found
     */
    public BitmapDrawable getDrawableFromPath(Context context , Uri path) {
        if (isMainThread()) {
            Glide.with(context)
                    .asBitmap()
                    .load(path)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            mBitmap = resource;
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                Glide.with(context)
                        .asBitmap()
                        .load(path)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                mBitmap = resource;
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            });
        }

        return new BitmapDrawable(context.getResources(), mBitmap);
    }
}
