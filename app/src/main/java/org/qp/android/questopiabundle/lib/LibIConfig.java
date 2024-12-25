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
    public int fontSize;
    public int backColor;
    public int fontColor;
    public int linkColor;

    public LibIConfig() {}

    protected LibIConfig(Parcel in) {
        useHtml = in.readByte() != 0;
        fontSize = in.readInt();
        backColor = in.readInt();
        fontColor = in.readInt();
        linkColor = in.readInt();
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
        dest.writeByte((byte) (useHtml ? 1 : 0));
        dest.writeInt(fontSize);
        dest.writeInt(backColor);
        dest.writeInt(fontColor);
        dest.writeInt(linkColor);
    }

    @NonNull
    @Override
    public String toString() {
        return "LibIConfig{" +
                "useHtml=" + useHtml +
                ", fontSize=" + fontSize +
                ", backColor=" + backColor +
                ", fontColor=" + fontColor +
                ", linkColor=" + linkColor +
                '}';
    }
}
