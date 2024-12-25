package org.qp.android.questopiabundle.lib;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class LibRefIRequest implements Parcelable {

    public static final Creator<LibRefIRequest> CREATOR = new Creator<>() {
        @Override
        public LibRefIRequest createFromParcel(Parcel in) {
            return new LibRefIRequest(in);
        }

        @Override
        public LibRefIRequest[] newArray(int size) {
            return new LibRefIRequest[size];
        }
    };

    public boolean isIConfigChanged;
    public boolean isMainDescChanged;
    public boolean isActionsChanged;
    public boolean isObjectsChanged;
    public boolean isVarsDescChanged;

    public LibRefIRequest() {
        isIConfigChanged = false;
        isMainDescChanged = false;
        isActionsChanged = false;
        isObjectsChanged = false;
        isVarsDescChanged = false;
    }

    protected LibRefIRequest(Parcel in) {
        isIConfigChanged = in.readByte() != 0;
        isMainDescChanged = in.readByte() != 0;
        isActionsChanged = in.readByte() != 0;
        isObjectsChanged = in.readByte() != 0;
        isVarsDescChanged = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByte((byte) (isIConfigChanged ? 1 : 0));
        dest.writeByte((byte) (isMainDescChanged ? 1 : 0));
        dest.writeByte((byte) (isActionsChanged ? 1 : 0));
        dest.writeByte((byte) (isObjectsChanged ? 1 : 0));
        dest.writeByte((byte) (isVarsDescChanged ? 1 : 0));
    }

    @NonNull
    @Override
    public String toString() {
        return "LibRefIRequest{" +
                "isIConfigChanged=" + isIConfigChanged +
                ", isMainDescChanged=" + isMainDescChanged +
                ", isActionsChanged=" + isActionsChanged +
                ", isObjectsChanged=" + isObjectsChanged +
                ", isVarsDescChanged=" + isVarsDescChanged +
                '}';
    }
}
