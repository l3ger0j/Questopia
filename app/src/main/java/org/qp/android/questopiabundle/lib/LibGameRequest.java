package org.qp.android.questopiabundle.lib;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public enum LibGameRequest implements Parcelable {
    LOAD_FILE,
    SAVE_FILE,
    USE_EXECUTOR,
    USE_INPUT,
    RESTART_GAME,
    EXECUTE_CODE,
    ;

    public static final Parcelable.Creator<LibGameRequest> CREATOR = new Parcelable.Creator<>() {
        @Override
        public LibGameRequest createFromParcel(Parcel in) {
            return LibGameRequest.valueOf(in.readString());
        }

        @Override
        public LibGameRequest[] newArray(int size) {
            return new LibGameRequest[size];
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
