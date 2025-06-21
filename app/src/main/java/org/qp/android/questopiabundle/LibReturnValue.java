package org.qp.android.questopiabundle;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.os.ParcelCompat;

public class LibReturnValue implements Parcelable {

    public static final Creator<LibReturnValue> CREATOR = new Creator<>() {
        @Override
        public LibReturnValue createFromParcel(Parcel in) {
            return new LibReturnValue(in);
        }

        @Override
        public LibReturnValue[] newArray(int size) {
            return new LibReturnValue[size];
        }
    };

    public String dialogTextValue = "";
    public int dialogNumValue = -1;
    public boolean playFileState = false;
    public Uri fileUri = Uri.EMPTY;

    protected LibReturnValue(Parcel in) {
        dialogTextValue = in.readString();
        dialogNumValue = in.readInt();
        playFileState = in.readInt() != 0;
        fileUri = ParcelCompat.readParcelable(in, Uri.class.getClassLoader(), Uri.class);
    }

    public LibReturnValue() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(dialogTextValue);
        dest.writeInt(dialogNumValue);
        dest.writeInt(playFileState ? 1 : 0);
        dest.writeParcelable(fileUri, flags);
    }
}