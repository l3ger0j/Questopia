package org.qp.android.model.libQP;

import android.net.Uri;

import org.qp.android.view.game.GameInterface;

import java.io.File;

public interface LibQpProxy {
    /**
     * Starts the library thread.
     */
    void start();

    /**
     * Stops the library thread.
     */
    void stop();

    void enableDebugMode (boolean isDebug);

    String getVersionQSP();
    String getCompiledDateTime();

    void runGame(String id, String title, File dir, File file);
    void restartGame();
    void loadGameState(Uri uri);
    void saveGameState(Uri uri);

    void onActionClicked(int index);
    void onObjectSelected(int index);
    void onInputAreaClicked();
    void onUseExecutorString();

    /**
     * Starts execution of the specified line of code in the library.
     */
    void execute(String code);

    /**
     * Starts processing the location counter in the library.
     */
    void executeCounter();

    GameState getGameState();

    void setGameInterface(GameInterface view);
}
