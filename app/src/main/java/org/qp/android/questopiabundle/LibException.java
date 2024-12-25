package org.qp.android.questopiabundle;

import android.os.Parcel;
import android.os.Parcelable;

public class LibException implements Parcelable {

    public static final int RUNTIME_EXCEPTION = 1000;
    public static final int ARITHMETIC_EXCEPTION = 1001;
    public static final Creator<LibException> CREATOR = new Creator<>() {
        @Override
        public LibException createFromParcel(Parcel in) {
            return new LibException(in);
        }

        @Override
        public LibException[] newArray(int size) {
            return new LibException[size];
        }
    };

    private final String errorMessage;
    private final int errorCode;

    public LibException(String errorMessage) {
        this(errorMessage, RUNTIME_EXCEPTION);
    }

    public LibException(String errorMessage, int errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    protected LibException(Parcel in) {
        errorMessage = in.readString();
        errorCode = in.readInt();
    }

    public Exception toException() {
        switch (errorCode) {
            case RUNTIME_EXCEPTION:
                return new RuntimeException(errorMessage);
            case ARITHMETIC_EXCEPTION:
                return new ArithmeticException(errorMessage);
            default:
                return new RuntimeException(errorMessage);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(errorMessage);
        dest.writeInt(errorCode);
    }
}
