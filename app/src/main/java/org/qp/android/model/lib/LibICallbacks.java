package org.qp.android.model.lib;

/**
 * Methods of this interface are called from native code. See <code>android_callbacks.c</code>.
 */
public interface LibICallbacks {

    void RefreshInt();
    void ShowPicture(String path);
    void SetTimer(int msecs);
    void ShowMessage(String message);
    void PlayFile(String path, int volume);
    boolean IsPlayingFile(final String path);
    void CloseFile(String path);
    void OpenGame(String filename);
    void SaveGame(String filename);
    String InputBox(String prompt);
    int GetMSCount();
    void AddMenuItem(String name, String imgPath);
    void ShowMenu();
    void DeleteMenu();
    void Wait(int msecs);
    void ShowWindow(int type, boolean isShow);
    byte[] GetFileContents(String path);
    void ChangeQuestPath(String path);

}
