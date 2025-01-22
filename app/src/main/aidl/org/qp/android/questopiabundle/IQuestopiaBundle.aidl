package org.qp.android.questopiabundle;

import android.net.Uri;
import org.qp.android.questopiabundle.LibResult;
import org.qp.android.questopiabundle.AsyncCallbacks;

interface IQuestopiaBundle {
    // API
    String versionPlugin();
    String titlePlugin();
    String authorPlugin();

    // Plugin part
    void startNativeLib(int libVer);
    void stopNativeLib(int libVer);

    void runGameIntoLib(long gameId, String gameTitle,in Uri gameDirUri,in Uri gameFileUri);
    void onActionClicked(int index);
    void onObjectClicked(int index);
    void doLibRequest(in LibResult gameRequest, String codeToExec, in Uri fileUri);

    void sendAsync(AsyncCallbacks callbacks);
}