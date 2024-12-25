package org.qp.android.questopiabundle.lib;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.qp.android.questopiabundle.dto.LibListItem;
import org.qp.android.questopiabundle.dto.LibMenuItem;

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

    public LibIConfig interfaceConfig;
    public boolean gameRunning;
    public long gameId;
    public String gameTitle;
    public Uri gameDirUri;
    public Uri gameFileUri;
    public String mainDesc;
    public String varsDesc;
    public List<LibListItem> actionsList = new ArrayList<>();
    public List<LibListItem> objectsList = new ArrayList<>();
    public List<LibMenuItem> menuItemsList = new ArrayList<>();

    public LibGameState() {
        interfaceConfig = new LibIConfig();
        mainDesc = "";
        varsDesc = "";
    }

    protected LibGameState(Parcel in) {
        interfaceConfig = in.readParcelable(LibIConfig.class.getClassLoader());
        gameRunning = in.readByte() != 0;
        gameId = in.readLong();
        gameTitle = in.readString();
        gameDirUri = in.readParcelable(Uri.class.getClassLoader());
        gameFileUri = in.readParcelable(Uri.class.getClassLoader());
        mainDesc = in.readString();
        varsDesc = in.readString();
        in.readTypedList(actionsList, LibListItem.CREATOR);
        in.readTypedList(objectsList, LibListItem.CREATOR);
        in.readTypedList(menuItemsList, LibMenuItem.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(interfaceConfig, flags);
        dest.writeByte((byte) (gameRunning ? 1 : 0));
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

    public void reset() {
        interfaceConfig.reset();
        gameRunning = false;
        gameId = 0L;
        gameTitle = "";
        gameDirUri = Uri.EMPTY;
        gameFileUri = Uri.EMPTY;
        mainDesc = "";
        varsDesc = "";
        actionsList = new ArrayList<>();
        objectsList = new ArrayList<>();
        menuItemsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public String toString() {
        return "LibGameState{" +
                "interfaceConfig=" + interfaceConfig +
                ", gameRunning=" + gameRunning +
                ", gameId=" + gameId +
                ", gameTitle='" + gameTitle + '\'' +
                ", gameDirUri=" + gameDirUri +
                ", gameFileUri=" + gameFileUri +
                ", mainDesc='" + mainDesc + '\'' +
                ", varsDesc='" + varsDesc + '\'' +
                ", actionsList=" + actionsList +
                ", objectsList=" + objectsList +
                ", menuItemsList=" + menuItemsList +
                '}';
    }
}
