package com.qsp.player.model.libQSP;

import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getFileContents;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.StringUtil.getStringOrEmpty;
import static com.qsp.player.utils.StringUtil.isNotEmpty;
import static com.qsp.player.utils.ThreadUtil.isSameThread;
import static com.qsp.player.utils.ThreadUtil.throwIfNotMainThread;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.qsp.player.R;
import com.qsp.player.dto.libQSP.ActionData;
import com.qsp.player.dto.libQSP.ErrorData;
import com.qsp.player.dto.libQSP.GetVarValuesResponse;
import com.qsp.player.dto.libQSP.ObjectData;
import com.qsp.player.model.service.AudioPlayer;
import com.qsp.player.model.service.GameContentResolver;
import com.qsp.player.model.service.HtmlProcessor;
import com.qsp.player.model.service.ImageProvider;
import com.qsp.player.utils.StreamUtil;
import com.qsp.player.view.game.GameInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class LibQspProxyImpl implements LibQspProxy, LibQspCallbacks {
    private final String TAG = this.getClass().getSimpleName();

    private final ReentrantLock libQspLock = new ReentrantLock();
    private final GameState gameState = new GameState();
    private final NativeMethods nativeMethods = new NativeMethods(this);

    private Thread libQspThread;
    private volatile Handler libQspHandler;
    private volatile boolean libQspThreadInit;
    private volatile long gameStartTime;
    private volatile long lastMsCountCallTime;
    private GameInterface gameInterface;

    private final Context context;
    private final GameContentResolver gameContentResolver;
    private final ImageProvider imageProvider;
    private final HtmlProcessor htmlProcessor;
    private final AudioPlayer audioPlayer;

    public LibQspProxyImpl(
            Context context,
            GameContentResolver gameContentResolver,
            ImageProvider imageProvider,
            HtmlProcessor htmlProcessor,
            AudioPlayer audioPlayer) {
        this.context = context;
        this.gameContentResolver = gameContentResolver;
        this.imageProvider = imageProvider;
        this.htmlProcessor = htmlProcessor;
        this.audioPlayer = audioPlayer;
    }

    private void runOnQspThread(final Runnable runnable) {
        throwIfNotMainThread();

        if (libQspThread == null) {
            Log.w(TAG,"libqsp thread has not been started");
            return;
        }
        if (!libQspThreadInit) {
            Log.w(TAG,"libqsp thread has been started, but not initialized");
            return;
        }
        Handler handler = libQspHandler;
        if (handler != null) {
            handler.post(() -> {
                libQspLock.lock();
                try {
                    runnable.run();
                } finally {
                    libQspLock.unlock();
                }
            });
        }
    }

    private boolean loadGameWorld() {
        byte[] gameData;
        try (FileInputStream in = new FileInputStream(gameState.gameFile)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to load the game world", ex);
            return false;
        }
        String fileName = gameState.gameFile.getAbsolutePath();
        if (!nativeMethods.QSPLoadGameWorldFromData(gameData, gameData.length, fileName)) {
            showLastQspError();
            return false;
        }

        return true;
    }

    private void showLastQspError() {
        ErrorData errorData = (ErrorData) nativeMethods.QSPGetLastErrorData();
        String locName = getStringOrEmpty(errorData.locName);
        String desc = getStringOrEmpty(nativeMethods.QSPGetErrorDesc(errorData.errorNum));

        final String message = String.format(
                Locale.getDefault(),
                "Location: %s\nAction: %d\nLine: %d\nError number: %d\nDescription: %s",
                locName,
                errorData.index,
                errorData.line,
                errorData.errorNum,
                desc);

        Log.e(TAG,message);

        GameInterface inter = gameInterface;
        if (inter != null) {
            gameInterface.showError(message);
        }
    }

    /**
     * Загружает конфигурацию интерфейса - использование HTML, шрифт и цвета - из библиотеки.
     *
     * @return <code>true</code> если конфигурация изменилась, иначе <code>false</code>
     */
    private boolean loadInterfaceConfiguration() {
        InterfaceConfiguration config = gameState.interfaceConfig;
        boolean changed = false;

        GetVarValuesResponse htmlResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("USEHTML", 0);
        if (htmlResult.success) {
            boolean useHtml = htmlResult.intValue != 0;
            if (config.useHtml != useHtml) {
                config.useHtml = useHtml;
                changed = true;
            }
        }
        GetVarValuesResponse fSizeResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.success && config.fontSize != fSizeResult.intValue) {
            config.fontSize = fSizeResult.intValue;
            changed = true;
        }
        GetVarValuesResponse bColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.success && config.backColor != bColorResult.intValue) {
            config.backColor = bColorResult.intValue;
            changed = true;
        }
        GetVarValuesResponse fColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.success && config.fontColor != fColorResult.intValue) {
            config.fontColor = fColorResult.intValue;
            changed = true;
        }
        GetVarValuesResponse lColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.success && config.linkColor != lColorResult.intValue) {
            config.linkColor = lColorResult.intValue;
            changed = true;
        }

        return changed;
    }

    private ArrayList<QspListItem> getActions() {
        ArrayList<QspListItem> actions = new ArrayList<>();
        int count = nativeMethods.QSPGetActionsCount();
        for (int i = 0; i < count; ++i) {
            ActionData actionData = (ActionData) nativeMethods.QSPGetActionData(i);
            QspListItem action = new QspListItem();
            action.icon = imageProvider.get(actionData.image);
            action.text = gameState.interfaceConfig.useHtml ?
                    htmlProcessor.removeHTMLTags(actionData.name) : actionData.name;
            actions.add(action);
        }
        return actions;
    }

    private ArrayList<QspListItem> getObjects() {
        ArrayList<QspListItem> objects = new ArrayList<>();
        int count = nativeMethods.QSPGetObjectsCount();
        for (int i = 0; i < count; i++) {
            ObjectData objectResult = (ObjectData) nativeMethods.QSPGetObjectData(i);
            QspListItem object = new QspListItem();
            object.icon = imageProvider.get(objectResult.image);
            object.text = gameState.interfaceConfig.useHtml ? htmlProcessor.removeHTMLTags(objectResult.name) : objectResult.name;
            objects.add(object);
        }
        return objects;
    }

    // region LibQspProxy

    public void start() {
        libQspThread = new Thread("libqsp") {
            @Override
            public void run() {
                try {
                    nativeMethods.QSPInit();
                    Looper.prepare();
                    libQspHandler = new Handler();
                    libQspThreadInit = true;

                    Looper.loop();

                    nativeMethods.QSPDeInit();
                } catch (Throwable t) {
                    Log.e(TAG,"libqsp thread has stopped exceptionally", t);
                }
            }
        };
        libQspThread.start();
    }

    public void stop() {
        throwIfNotMainThread();

        if (libQspThread == null) return;

        if (libQspThreadInit) {
            Handler handler = libQspHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            libQspThreadInit = false;
        } else {
            Log.w(TAG,"libqsp thread has been started, but not initialized");
        }
        libQspThread = null;
    }

    public void enableDebugMode (boolean isDebug) {
        runOnQspThread(() -> nativeMethods.QSPEnableDebugMode(isDebug));
    }

    @Override
    public void runGame(final String id, final String title, final File dir, final File file) {
        runOnQspThread(() -> doRunGame(id, title, dir, file));
    }

    private void doRunGame(final String id, final String title, final File dir, final File file) {
        gameInterface.doWithCounterDisabled(() -> {
            audioPlayer.closeAllFiles();

            gameState.reset();
            gameState.gameRunning = true;
            gameState.gameId = id;
            gameState.gameTitle = title;
            gameState.gameDir = dir;
            gameState.gameFile = file;

            gameContentResolver.setGameDir(dir);

            if (!loadGameWorld()) return;

            gameStartTime = SystemClock.elapsedRealtime();
            lastMsCountCallTime = 0;

            if (!nativeMethods.QSPRestartGame(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void restartGame() {
        runOnQspThread(() -> {
            GameState state = gameState;
            doRunGame(state.gameId, state.gameTitle, state.gameDir, state.gameFile);
        });
    }

    @Override
    public void loadGameState(final Uri uri) {
        if (!isSameThread(libQspHandler.getLooper().getThread())) {
            runOnQspThread(() -> loadGameState(uri));
            return;
        }
        final byte[] gameData;

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
                Log.d(TAG, Arrays.toString(gameData));
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to load game state", ex);
            return;
        }

        if (!nativeMethods.QSPOpenSavedGameFromData(gameData, gameData.length, true)) {
            showLastQspError();
        }
    }

    @Override
    public void saveGameState(final Uri uri) {
        if (!isSameThread(libQspHandler.getLooper().getThread())) {
            runOnQspThread(() -> saveGameState(uri));
            return;
        }
        byte[] gameData = nativeMethods.QSPSaveGameAsData(false);
        if (gameData == null) return;

        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "w")) {
            out.write(gameData);
        } catch (IOException ex) {
            Log.e(TAG,"Failed to save the game state", ex);
        }
    }

    @Override
    public void onActionSelected(final int index) {
        runOnQspThread(() -> {
            if (!nativeMethods.QSPSetSelActionIndex(index, true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void onActionClicked(final int index) {
        runOnQspThread(() -> {
            if (!nativeMethods.QSPSetSelActionIndex(index, false)) {
                showLastQspError();
            }
            if (!nativeMethods.QSPExecuteSelActionCode(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void onObjectSelected(final int index) {
        runOnQspThread(() -> {
            if (!nativeMethods.QSPSetSelObjectIndex(index, true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void onInputAreaClicked() {
        final GameInterface inter = gameInterface;
        if (inter == null) return;

        runOnQspThread(() -> {
            String input = inter.showInputBox(context.getString(R.string.userInputTitle));
            nativeMethods.QSPSetInputStrText(input);

            if (!nativeMethods.QSPExecUserInput(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void execute(final String code) {
        runOnQspThread(() -> {
            if (!nativeMethods.QSPExecString(code, true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void executeCounter() {
        if (libQspLock.isLocked()) return;

        runOnQspThread(() -> {
            if (!nativeMethods.QSPExecCounter(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public void setGameInterface(GameInterface view) {
        gameInterface = view;
    }

    // endregion LibQspProxy

    // region LibQspCallbacks

    @Override
    public void RefreshInt() {
        RefreshInterfaceRequest request = new RefreshInterfaceRequest();

        boolean configChanged = loadInterfaceConfiguration();
        if (configChanged) {
            request.interfaceConfigChanged = true;
        }
        if (nativeMethods.QSPIsMainDescChanged()) {
            gameState.mainDesc = nativeMethods.QSPGetMainDesc();
            request.mainDescChanged = true;
        }
        if (nativeMethods.QSPIsActionsChanged()) {
            gameState.actions = getActions();
            request.actionsChanged = true;
        }
        if (nativeMethods.QSPIsObjectsChanged()) {
            gameState.objects = getObjects();
            request.objectsChanged = true;
        }
        if (nativeMethods.QSPIsVarsDescChanged()) {
            gameState.varsDesc = nativeMethods.QSPGetVarsDesc();
            request.varsDescChanged = true;
        }

        GameInterface inter = gameInterface;
        if (inter != null) {
            inter.refresh(request);
        }
    }

    @Override
    public void ShowPicture(String path) {
        GameInterface inter = gameInterface;
        if (inter != null && isNotEmpty(path)) {
            inter.showPicture(path);
        }
    }

    @Override
    public void SetTimer(int msecs) {
        GameInterface inter = gameInterface;
        if (inter != null) {
            inter.setCounterInterval(msecs);
        }
    }

    @Override
    public void ShowMessage(String message) {
        GameInterface inter = gameInterface;
        if (inter != null) {
            inter.showMessage(message);
        }
    }

    @Override
    public void PlayFile(String path, int volume) {
        if (isNotEmpty(path)) {
            audioPlayer.playFile(path, volume);
        }
    }

    @Override
    public boolean IsPlayingFile(final String path) {
        return isNotEmpty(path) && audioPlayer.isPlayingFile(path);
    }

    @Override
    public void CloseFile(String path) {
        if (isNotEmpty(path)) {
            audioPlayer.closeFile(path);
        } else {
            audioPlayer.closeAllFiles();
        }
    }

    @Override
    public void OpenGame(String filename) {
        GameInterface inter = gameInterface;
        File savesDir = getOrCreateDirectory(gameState.gameDir, "saves");
        File saveFile = findFileOrDirectory(savesDir, filename);
        if (saveFile == null && inter != null) {
            Log.e(TAG,"Save file not found: " + filename);
            inter.showLoadGamePopup();
            return;
        }
        if (inter != null) {
            inter.doWithCounterDisabled(() -> loadGameState(Uri.fromFile(saveFile)));
        }
    }

    @Override
    public void SaveGame(String filename) {
        GameInterface inter = gameInterface;
        if (inter != null) {
            inter.showSaveGamePopup(filename);
        }
    }

    @Override
    public String InputBox(String prompt) {
        GameInterface inter = gameInterface;
        return inter != null ? inter.showInputBox(prompt) : null;
    }

    @Override
    public int GetMSCount() {
        long now = SystemClock.elapsedRealtime();
        if (lastMsCountCallTime == 0) {
            lastMsCountCallTime = gameStartTime;
        }
        int dt = (int) (now - lastMsCountCallTime);
        lastMsCountCallTime = now;

        return dt;
    }

    @Override
    public void AddMenuItem(String name, String imgPath) {
        QspMenuItem item = new QspMenuItem();
        item.imgPath = imgPath;
        item.name = name;
        gameState.menuItems.add(item);
    }

    @Override
    public void ShowMenu() {
        GameInterface inter = gameInterface;
        if (inter == null) return;

        int result = inter.showMenu();
        if (result != -1) {
            nativeMethods.QSPSelectMenuItem(result);
        }
    }

    @Override
    public void DeleteMenu() {
        gameState.menuItems.clear();
    }

    @Override
    public void Wait(int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException ex) {
            Log.e(TAG,"Wait failed", ex);
        }
    }

    @Override
    public void ShowWindow(int type, boolean isShow) {
        GameInterface inter = gameInterface;
        if (inter != null) {
            WindowType windowType = WindowType.values()[type];
            inter.showWindow(windowType, isShow);
        }
    }

    @Override
    public byte[] GetFileContents(String path) {
        return getFileContents(path);
    }

    @Override
    public void ChangeQuestPath(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            Log.e(TAG,"GameData directory not found: " + path);
            return;
        }
        if (!gameState.gameDir.equals(dir)) {
            gameState.gameDir = dir;
            gameContentResolver.setGameDir(dir);
        }
    }

    // endregion LibQspCallbacks
}
