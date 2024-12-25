package org.qp.android.questopiabundle;

import android.os.Parcel;
import android.os.Parcelable;

public class LibResult<T extends Parcelable> implements Parcelable {

    public static final Creator<LibResult<Parcelable>> CREATOR = new Creator<>() {
        @Override
        public LibResult<Parcelable> createFromParcel(Parcel in) {
            return new LibResult<>(in);
        }

        @Override
        public LibResult<Parcelable>[] newArray(int size) {
            return new LibResult[size];
        }
    };

    public T value;

    public LibResult(T value) {
        this.value = value;
    }

    // to reconstruct object
    protected LibResult(Parcel in) {
        this.value = in.readParcelable(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(value, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
