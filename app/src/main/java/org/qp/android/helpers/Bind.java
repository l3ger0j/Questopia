package org.qp.android.helpers;

import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import org.qp.android.R;

import java.io.File;

public class Bind {
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (view.getId() == R.id.imageBox) {
                Picasso.get()
                        .load(new File(imageUrl))
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
}
