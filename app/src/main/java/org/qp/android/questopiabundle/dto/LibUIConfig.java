package org.qp.android.questopiabundle.dto;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibUIConfig implements Parcelable {

    public static final Creator<LibUIConfig> CREATOR = new Creator<>() {
        @Override
        public LibUIConfig createFromParcel(Parcel in) {
            return new LibUIConfig(in);
        }

        @Override
        public LibUIConfig[] newArray(int size) {
            return new LibUIConfig[size];
        }
    };

    public boolean useHtml = false;
    public long fontSize = 0L;
    public long backColor = 0L;
    public long fontColor = 0L;
    public long linkColor = 0L;

    public LibUIConfig() {
    }

    protected LibUIConfig(Parcel in) {
        useHtml = in.readInt() != 0;
        fontSize = in.readLong();
        backColor = in.readLong();
        fontColor = in.readLong();
        linkColor = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(useHtml ? 1 : 0);
        dest.writeLong(fontSize);
        dest.writeLong(backColor);
        dest.writeLong(fontColor);
        dest.writeLong(linkColor);
    }

}