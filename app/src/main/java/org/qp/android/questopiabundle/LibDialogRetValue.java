package org.qp.android.questopiabundle;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibDialogRetValue implements Parcelable {

    public static final Creator<LibDialogRetValue> CREATOR = new Creator<>() {
        @Override
        public LibDialogRetValue createFromParcel(Parcel in) {
            return new LibDialogRetValue(in);
        }

        @Override
        public LibDialogRetValue[] newArray(int size) {
            return new LibDialogRetValue[size];
        }
    };

    public String outTextValue;
    public int outNumValue;

    protected LibDialogRetValue(Parcel in) {
        outTextValue = in.readString();
        outNumValue = in.readInt();
    }

    public LibDialogRetValue() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(outTextValue);
        dest.writeInt(outNumValue);
    }
}
