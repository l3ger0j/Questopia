package com.qsp.player.libqsp;

import android.net.Uri;

import com.qsp.player.libqsp.model.GameState;

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
    void restartGame();
    void loadGameState(Uri uri);
    void saveGameState(Uri uri);

    void onActionSelected(int index);
    void onActionClicked(int index);
    void onObjectSelected(int index);
    void onInputAreaClicked();

    /**
     * Запускает выполнение указанной строки кода в библиотеке.
     */
    void execute(String code);

    /**
     * Запускает обработку локации-счётчика в библиотеке.
     */
    void executeCounter();

    GameState getGameState();

    void setGameInterface(GameInterface view);
}
