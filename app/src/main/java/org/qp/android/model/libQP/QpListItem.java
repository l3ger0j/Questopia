package org.qp.android.model.libQP;

import java.util.Objects;

public class QpListItem {
    public String pathToImage;
    public CharSequence text;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (QpListItem) o;
        return Objects.equals(text, that.text)
                && Objects.equals(pathToImage, that.pathToImage);
    }
}
