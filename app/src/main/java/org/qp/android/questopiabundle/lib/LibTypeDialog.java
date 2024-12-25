package org.qp.android.questopiabundle.lib;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum LibTypeDialog implements Parcelable {
    DIALOG_ERROR,
    DIALOG_PICTURE,
    DIALOG_MESSAGE,
    DIALOG_INPUT,
    DIALOG_EXECUTOR,
    DIALOG_MENU,
    DIALOG_POPUP_SAVE,
    DIALOG_POPUP_LOAD,
    ;

    public static final Creator<LibTypeDialog> CREATOR = new Creator<>() {
        @Override
        public LibTypeDialog createFromParcel(Parcel in) {
            return LibTypeDialog.valueOf(in.readString());
        }

        @Override
        public LibTypeDialog[] newArray(int size) {
            return new LibTypeDialog[size];
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
