package org.qp.android.view.adapters;

import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import org.qp.android.R;

import java.io.File;

public class Bind {
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String imageUrl) {
        if (imageUrl != null) {
            if (imageUrl.isEmpty()) {
                var drawable = ResourcesCompat.getDrawable(
                        view.getContext().getResources() ,
                        R.drawable.broken_image , null
                );
                view.setImageDrawable(drawable);
            } else {
                Picasso.get()
                        .load(new File(imageUrl))
                        .into(view);
            }
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources() ,
                    R.drawable.broken_image , null
            );
            view.setImageDrawable(drawable);
        }
    }
}
