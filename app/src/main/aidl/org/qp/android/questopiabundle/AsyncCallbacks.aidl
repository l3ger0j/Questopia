package org.qp.android.questopiabundle;

import android.net.Uri;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibDialogRetValue;

interface AsyncCallbacks {
    void updateState(in LibResult refReq, in LibResult newState);
    void changeGameDir(in String filePath);

    LibDialogRetValue doOnShowDialog(in LibResult typeDialog, String inputString);
    void doChangeVisWindow(in LibResult typeWindow, boolean isShow);

    boolean isPlayingFile(String filePath);
    void closeAllFiles();
    void closeFile(String filePath);
    void playFile(String path, int volume);

    Uri requestReceiveFile(in String filePath);
    Uri requestCreateFile(in String path);

    void onError(in LibException libException);
}