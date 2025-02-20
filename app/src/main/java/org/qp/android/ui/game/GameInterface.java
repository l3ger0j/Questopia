package org.qp.android.ui.game;

import com.libqsp.jni.QSPLib;

import org.qp.android.model.lib.LibRefIRequest;
import org.qp.android.model.lib.LibWindowType;

import java.util.List;

public interface GameInterface {

    void refresh(LibRefIRequest request);

    void showErrorDialog(String message);

    void showPicture(String path);

    void showMessage(String message);

    String showInputDialog(String prompt);

    String showExecutorDialog(String prompt);

    int showMenu(List<QSPLib.ListItem> items);

    void showLoadGamePopup();

    void showSaveGamePopup();

    void showWindow(LibWindowType type, boolean show);

    /**
     * Set the counter location processing interval to <code>millis</code> milliseconds.
     */
    void setCounterInterval(int millis);

    /**
     * Execute <code>runnable</code> without processing the location counter.
     */
    void doWithCounterDisabled(Runnable runnable);
}
