package com.qsp.player.libqsp;

import com.qsp.player.libqsp.model.RefreshInterfaceRequest;
import com.qsp.player.libqsp.model.WindowType;

public interface GameInterface {

    void refresh(RefreshInterfaceRequest request);

    void showError(String message);
    void showPicture(String path);
    void showMessage(String message);
    String showInputBox(String prompt);
    int showMenu();
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
