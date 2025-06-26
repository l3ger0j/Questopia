package org.qp.android.helpers;

import static org.qp.android.helpers.utils.UriUtil.isNotEmptyOrBlanks;

import android.net.Uri;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.BindingAdapter;

import com.bumptech.glide.Glide;

import org.qp.android.R;

import java.util.Objects;

public class Bind {

    @BindingAdapter({"imageUri"})
    public static void loadImage(ImageView view, Uri imageUri) {
        if (isNotEmptyOrBlanks(imageUri)) {
            var scheme = imageUri.getScheme();
            if (Objects.equals(scheme, "http") || Objects.equals(scheme, "https")) {
                Glide.with(view).load(imageUri).centerCrop().into(view);
            }  else {
                view.setImageURI(imageUri);
            }
        } else {
            var drawable = ResourcesCompat.getDrawable(
                    view.getContext().getResources(),
                    R.drawable.baseline_broken_image_24,
                    null
            );
            view.setImageDrawable(drawable);
        }
    }

}
