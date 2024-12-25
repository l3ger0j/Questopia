package org.qp.android.questopiabundle.dto;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibMenuItem implements Parcelable {

    public static final Creator<LibMenuItem> CREATOR = new Creator<>() {
        @Override
        public LibMenuItem createFromParcel(Parcel in) {
            return new LibMenuItem(in);
        }

        @Override
        public LibMenuItem[] newArray(int size) {
            return new LibMenuItem[size];
        }
    };

    public String name;
    public String pathToImage;

    public LibMenuItem() {
        this.name = "";
        this.pathToImage = "";
    }

    protected LibMenuItem(Parcel in) {
        this.name = in.readString();
        this.pathToImage = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(pathToImage);
    }
}
