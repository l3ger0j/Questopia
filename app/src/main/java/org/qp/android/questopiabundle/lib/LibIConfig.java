package org.qp.android.questopiabundle.lib;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibIConfig implements Parcelable {

    public static final Creator<LibIConfig> CREATOR = new Creator<>() {
        @Override
        public LibIConfig createFromParcel(Parcel in) {
            return new LibIConfig(in);
        }

        @Override
        public LibIConfig[] newArray(int size) {
            return new LibIConfig[size];
        }
    };

    public boolean useHtml;
    public long fontSize;
    public long backColor;
    public long fontColor;
    public long linkColor;

    public LibIConfig() {}

    protected LibIConfig(Parcel in) {
        useHtml = in.readInt() != 0;
        fontSize = in.readLong();
        backColor = in.readLong();
        fontColor = in.readLong();
        linkColor = in.readLong();
    }

    public void reset() {
        useHtml = false;
        fontSize = 0;
        backColor = 0;
        fontColor = 0;
        linkColor = 0;
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
