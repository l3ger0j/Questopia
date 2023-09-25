package org.qp.android.ui.game;

import org.qp.android.model.libQP.RefreshInterfaceRequest;
import org.qp.android.model.libQP.WindowType;

public interface GameInterface {

    void refresh(RefreshInterfaceRequest request);
    void showError(String message);
    void showPicture(String path);
    void showMessage(String message);
    String showInputDialog(String prompt);
    String showExecutorDialog(String prompt);
    int showMenu();
    void showLoadGamePopup();
    void showSaveGamePopup();
    void showWindow(WindowType type, boolean show);

    /**
     * Set the counter location processing interval to <code>millis</code> milliseconds.
     */
    void setCounterInterval(int millis);

    /**
     * Execute <code>runnable</code> without processing the location counter.
     */
    void doWithCounterDisabled(Runnable runnable);
}
