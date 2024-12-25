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
        if (isNotEmptyOrBlank(imageUrl)) {
            switch (view.getId()) {
                case R.id.imageBox -> Picasso.get()
                        .load(imageUrl)
                        .into(view);
                case R.id.game_icon -> Picasso.get()
                        .load(Uri.parse(imageUrl))
                        .fit()
                        .error(R.drawable.baseline_broken_image_24)
                        .into(view);
                default -> Picasso.get()
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
    public static void loadImage(ImageView view, Uri imageUri) {
        if (isNotEmptyOrBlank(String.valueOf(imageUri))) {
            Picasso.get().load(imageUri).fit().into(view);
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources() ,
                    R.drawable.baseline_broken_image_24 , null
            );
            view.setImageDrawable(drawable);
        }
    }

}
