package com.qsp.player.libqsp;

import com.qsp.player.libqsp.model.WindowType;

public interface PlayerView {

    void refreshGameView(
            boolean confChanged,
            boolean mainDescChanged,
            boolean actionsChanged,
            boolean objectsChanged,
            boolean varsDescChanged);

    void showError(String message);
    void showPicture(String path);
    void showMessage(String message);
    String showInputBox(String prompt);
    int showMenu();
    void showSaveGamePopup(String filename);
    void showWindow(WindowType type, boolean show);
}
