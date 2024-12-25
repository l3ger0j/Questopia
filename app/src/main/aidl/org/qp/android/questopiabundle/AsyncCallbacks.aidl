package org.qp.android.questopiabundle;

import android.net.Uri;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.LibException;
import org.qp.android.questopiabundle.LibDialogRetValue;

interface AsyncCallbacks {
    void sendLibGameState(in LibResult libResult);
    void sendLibRef(in LibResult libResult);
    void sendChangeCurrGameDir(in Uri gameDirUri);

    LibDialogRetValue doOnShowDialog(in LibResult typeDialog, String inputString);
    void doChangeVisWindow(in LibResult typeWindow, boolean isShow);

    boolean isPlayingFile(String filePath);
    void closeAllFiles();
    void closeFile(String filePath);
    void playFile(String path, int volume);

    void onError(in LibException libException);
}