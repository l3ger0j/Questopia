package org.qp.android.questopiabundle.lib;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum LibTypeWindow implements Parcelable {
    ACTIONS,
    OBJECTS,
    VARIABLES,
    INPUT
    ;

    public static final Creator<LibTypeWindow> CREATOR = new Creator<>() {
        @Override
        public LibTypeWindow createFromParcel(Parcel in) {
            return LibTypeWindow.valueOf(in.readString());
        }

        @Override
        public LibTypeWindow[] newArray(int size) {
            return new LibTypeWindow[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name());
    }
}
