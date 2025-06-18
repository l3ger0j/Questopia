package org.qp.android.questopiabundle;

import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.LibException;

interface AsyncCallbacks {
    void updateState(in LibResult refReq, in LibResult newState);
    void changeGameDir(in String filePath);

    void doShowDialog(in LibResult typeDialog, String inputString);
    void doChangeVisWindow(in LibResult typeWindow, boolean isShow);

    void isPlayingFile(String filePath);
    void closeAllFiles();
    void closeFile(String filePath);
    void playFile(String path, int volume);

    void requestReceiveFile(in String filePath);
    void requestCreateFile(in String path, in String mimeType);

    void onError(in LibException libException);
}