package org.qp.android.questopiabundle.dto;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibGenItem implements Parcelable {
    public static final Parcelable.Creator<LibGenItem> CREATOR = new Parcelable.Creator<>() {
        @Override
        public LibGenItem createFromParcel(Parcel in) {
            return new LibGenItem(in);
        }

        @Override
        public LibGenItem[] newArray(int size) {
            return new LibGenItem[size];
        }
    };

    public String text = "";
    public String imagePath = "";

    protected LibGenItem(Parcel in) {
        text = in.readString();
        imagePath = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeString(imagePath);
    }
}