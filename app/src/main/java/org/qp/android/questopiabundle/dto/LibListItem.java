package org.qp.android.questopiabundle.dto;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

public class LibListItem implements Parcelable {

    public static final Creator<LibListItem> CREATOR = new Creator<>() {
        @Override
        public LibListItem createFromParcel(Parcel in) {
            return new LibListItem(in);
        }

        @Override
        public LibListItem[] newArray(int size) {
            return new LibListItem[size];
        }
    };

    public String text;
    public String pathToImage;

    public LibListItem() {
        text = "";
        pathToImage = "";
    }

    protected LibListItem(Parcel in) {
        text = in.readString();
        pathToImage = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (LibListItem) o;
        return Objects.equals(text, that.text)
                && Objects.equals(pathToImage, that.pathToImage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(pathToImage);
    }
}
