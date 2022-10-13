package com.qsp.player.model.libQSP;

import android.net.Uri;

import com.qsp.player.view.activities.GameInterface;

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

    void enableDebugMode (boolean isDebug);

    void runGame(String id, String title, File dir, File file);
    void restartGame();
    void loadGameState(Uri uri);
    void saveGameState(Uri uri);

    byte[] getSaveGameState();
    void loadGameStateAsArray(byte[] saveData);

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
