package org.qp.android.questopiabundle.dto;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class LibGameState implements Parcelable {

    public static final Creator<LibGameState> CREATOR = new Creator<>() {
        @Override
        public LibGameState createFromParcel(Parcel in) {
            return new LibGameState(in);
        }

        @Override
        public LibGameState[] newArray(int size) {
            return new LibGameState[size];
        }
    };

    public boolean gameRunning = false;
    public long gameId = 0L;
    public String gameTitle = "";
    public Uri gameDirUri = Uri.EMPTY;
    public Uri gameFileUri = Uri.EMPTY;
    public String mainDesc = "";
    public String varsDesc = "";
    public List<LibGenItem> actionsList = new ArrayList<>();
    public List<LibGenItem> objectsList = new ArrayList<>();
    public List<LibGenItem> menuItemsList = new ArrayList<>();

    public LibGameState() {
    }

    protected LibGameState(Parcel in) {
        gameRunning = in.readInt() != 0;
        gameId = in.readLong();
        gameTitle = in.readString();
        gameDirUri = in.readParcelable(Uri.class.getClassLoader());
        gameFileUri = in.readParcelable(Uri.class.getClassLoader());
        mainDesc = in.readString();
        varsDesc = in.readString();
        in.readTypedList(actionsList, LibGenItem.CREATOR);
        in.readTypedList(objectsList, LibGenItem.CREATOR);
        in.readTypedList(menuItemsList, LibGenItem.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(gameRunning ? 1 : 0);
        dest.writeLong(gameId);
        dest.writeString(gameTitle);
        dest.writeParcelable(gameDirUri, flags);
        dest.writeParcelable(gameFileUri, flags);
        dest.writeString(mainDesc);
        dest.writeString(varsDesc);
        dest.writeTypedList(actionsList);
        dest.writeTypedList(objectsList);
        dest.writeTypedList(menuItemsList);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}