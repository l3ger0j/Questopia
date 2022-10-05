package com.qsp.player.view.activities;

import com.qsp.player.model.libQSP.RefreshInterfaceRequest;
import com.qsp.player.model.libQSP.WindowType;

public interface GameInterface {

    void refresh(RefreshInterfaceRequest request);
    void showError(String message);
    void showPicture(String path);
    void showMessage(String message);
    String showInputBox(String prompt);
    int showMenu();
    void showLoadGamePopup();
    void showSaveGamePopup(String filename);
    void showWindow(WindowType type, boolean show);

    // region Локация-счётчик

    /**
     * Установить интервал обработки локации-счётчика в <code>millis</code> миллисекунд.
     */
    void setCounterInterval(int millis);

    /**
     * Выполнить <code>runnable</code> без обработки локации-счётчика.
     */
    void doWithCounterDisabled(Runnable runnable);

    // endregion Локация-счётчик
}
