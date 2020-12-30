package com.qsp.player.libqsp;

import android.net.Uri;

import java.io.File;

public interface LibQspProxy {
    /**
     * Запускает поток библиотеки.
     */
    void start();

    /**
     * Останавливает поток библиотеки.
     */
    void stop();

    void runGame(String id, String title, File dir, File file);
    void pauseGame();
    void resumeGame();
    void restartGame();

    void loadGameState(Uri uri);
    void saveGameState(Uri uri);

    void execute(String code);

    void onActionSelected(int index);
    void onActionClicked(int index);
    void onObjectSelected(int index);
    void onInputAreaClicked();

    PlayerViewState getViewState();

    void setPlayerView(PlayerView view);
}
