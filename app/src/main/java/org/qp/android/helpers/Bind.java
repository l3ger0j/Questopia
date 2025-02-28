package org.qp.android.helpers;

import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.net.Uri;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import org.qp.android.R;

public class Bind {
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (view.getId() == R.id.imageBox) {
                Picasso.get()
                        .load(imageUrl)
                        .into(view);
            } else {
                Picasso.get()
                        .load(imageUrl)
                        .fit()
                        .into(view);
            }
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources() ,
                    R.drawable.baseline_broken_image_24 , null
            );
            view.setImageDrawable(drawable);
        }
    }

    @BindingAdapter({"imageUri"})
    public static void loadImage(ImageView view, Uri imageUrl) {
        if (isNotEmptyOrBlank(String.valueOf(imageUrl)) && imageUrl != Uri.EMPTY) {
            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .into(view);
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources() ,
                    R.drawable.baseline_broken_image_24 , null
            );
            view.setImageDrawable(drawable);
        }
    }
}
