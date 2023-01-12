package org.qp.android.model.libQSP;

import static org.qp.android.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.utils.FileUtil.getFileContents;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;
import static org.qp.android.utils.StringUtil.getStringOrEmpty;
import static org.qp.android.utils.StringUtil.isNotEmpty;
import static org.qp.android.utils.ThreadUtil.isSameThread;
import static org.qp.android.utils.ThreadUtil.throwIfNotMainThread;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import org.qp.android.R;
import org.qp.android.dto.libQSP.ActionData;
import org.qp.android.dto.libQSP.ErrorData;
import org.qp.android.dto.libQSP.GetVarValuesResponse;
import org.qp.android.dto.libQSP.ObjectData;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.model.service.ImageProvider;
import org.qp.android.utils.StreamUtil;
import org.qp.android.view.game.GameInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
        var handler = libQspHandler;
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
        try (var in = new FileInputStream(gameState.gameFile)) {
            try (var out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to load the game world", ex);
            return false;
        }
        var fileName = gameState.gameFile.getAbsolutePath();
        if (!nativeMethods.QSPLoadGameWorldFromData(gameData, gameData.length, fileName)) {
            showLastQspError();
            return false;
        }

        return true;
    }

    private void showLastQspError() {
        var errorData = (ErrorData) nativeMethods.QSPGetLastErrorData();
        var locName = getStringOrEmpty(errorData.locName);
        var desc = getStringOrEmpty(nativeMethods.QSPGetErrorDesc(errorData.errorNum));

        final var message = String.format(
                Locale.getDefault(),
                "Location: %s\nAction: %d\nLine: %d\nError number: %d\nDescription: %s",
                locName,
                errorData.index,
                errorData.line,
                errorData.errorNum,
                desc);

        Log.e(TAG,message);

        var inter = gameInterface;
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
        var config = gameState.interfaceConfig;
        boolean changed = false;

        var htmlResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("USEHTML", 0);
        if (htmlResult.isSuccess) {
            boolean useHtml = htmlResult.intValue != 0;
            if (config.useHtml != useHtml) {
                config.useHtml = useHtml;
                changed = true;
            }
        }
        var fSizeResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.isSuccess && config.fontSize != fSizeResult.intValue) {
            config.fontSize = fSizeResult.intValue;
            changed = true;
        }
        var bColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.isSuccess && config.backColor != bColorResult.intValue) {
            config.backColor = bColorResult.intValue;
            changed = true;
        }
        var fColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.isSuccess && config.fontColor != fColorResult.intValue) {
            config.fontColor = fColorResult.intValue;
            changed = true;
        }
        var lColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.isSuccess && config.linkColor != lColorResult.intValue) {
            config.linkColor = lColorResult.intValue;
            changed = true;
        }

        return changed;
    }

    private ArrayList<QpListItem> getActions() {
        ArrayList<QpListItem> actions = new ArrayList<>();
        var count = nativeMethods.QSPGetActionsCount();
        for (int i = 0; i < count; ++i) {
            var actionData = (ActionData) nativeMethods.QSPGetActionData(i);
            var action = new QpListItem();
            action.icon = imageProvider.get(actionData.image);
            action.text = gameState.interfaceConfig.useHtml ?
                    htmlProcessor.removeHTMLTags(actionData.name) : actionData.name;
            actions.add(action);
        }
        return actions;
    }

    @NonNull
    private ArrayList<QpListItem> getObjects() {
        ArrayList<QpListItem> objects = new ArrayList<>();
        var count = nativeMethods.QSPGetObjectsCount();
        for (int i = 0; i < count; i++) {
            var objectResult = (ObjectData) nativeMethods.QSPGetObjectData(i);
            var object = new QpListItem();
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
            var handler = libQspHandler;
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

    public String getVersionQSP () {
        if (!isSameThread(libQspHandler.getLooper().getThread())) {
            runOnQspThread(this::getVersionQSP);
        }
        return nativeMethods.QSPGetVersion();
    }

    public String getCompiledDateTime () {
        if (!isSameThread(libQspHandler.getLooper().getThread())) {
            runOnQspThread(this::getCompiledDateTime);
        }
        return nativeMethods.QSPGetCompiledDateTime();
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
            var state = gameState;
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
        try (var in = context.getContentResolver().openInputStream(uri)) {
            try (var out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
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
        try (var out = context.getContentResolver().openOutputStream(uri, "w")) {
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
        final var inter = gameInterface;
        if (inter == null) return;
        runOnQspThread(() -> {
            var input = inter.showInputDialog(context.getString(R.string.userInputTitle));
            nativeMethods.QSPSetInputStrText(input);
            if (!nativeMethods.QSPExecUserInput(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void onUseExecutorString() {
        final var inter = gameInterface;
        if (inter == null) return;
        runOnQspThread(() -> {
            var input = inter.showExecutorDialog(context.getString(R.string.execStringTitle));
            if (!nativeMethods.QSPExecString(input, true)) {
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
        var request = new RefreshInterfaceRequest();

        var configChanged = loadInterfaceConfiguration();
        if (configChanged) {
            request.interfaceConfigChanged = true;
        }
        if (nativeMethods.QSPIsMainDescChanged()) {
            if (!gameState.mainDesc.equals(nativeMethods.QSPGetMainDesc())) {
                gameState.mainDesc = nativeMethods.QSPGetMainDesc();
                request.mainDescChanged = true;
            }
        }
        if (nativeMethods.QSPIsActionsChanged()) {
            if (gameState.actions != getActions()) {
                gameState.actions = getActions();
                request.actionsChanged = true;
            }
        }
        if (nativeMethods.QSPIsObjectsChanged()) {
            if (gameState.objects != getObjects()) {
                gameState.objects = getObjects();
                request.objectsChanged = true;
            }
        }
        if (nativeMethods.QSPIsVarsDescChanged()) {
            if (!gameState.varsDesc.equals(nativeMethods.QSPGetVarsDesc())) {
                gameState.varsDesc = nativeMethods.QSPGetVarsDesc();
                request.varsDescChanged = true;
            }
        }

        var inter = gameInterface;
        if (inter != null) {
            inter.refresh(request);
        }
    }

    @Override
    public void ShowPicture(String path) {
        var inter = gameInterface;
        if (inter != null && isNotEmpty(path)) {
            inter.showPicture(path);
        }
    }

    @Override
    public void SetTimer(int msecs) {
        var inter = gameInterface;
        if (inter != null) {
            inter.setCounterInterval(msecs);
        }
    }

    @Override
    public void ShowMessage(String message) {
        var inter = gameInterface;
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
        var inter = gameInterface;
        var savesDir = getOrCreateDirectory(gameState.gameDir, "saves");
        var saveFile = findFileOrDirectory(savesDir, filename);
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
        var inter = gameInterface;
        if (inter != null) {
            inter.showSaveGamePopup(filename);
        }
    }

    @Override
    public String InputBox(String prompt) {
        var inter = gameInterface;
        return inter != null ? inter.showInputDialog(prompt) : null;
    }

    @Override
    public int GetMSCount() {
        var now = SystemClock.elapsedRealtime();
        if (lastMsCountCallTime == 0) {
            lastMsCountCallTime = gameStartTime;
        }
        var dt = (int) (now - lastMsCountCallTime);
        lastMsCountCallTime = now;
        return dt;
    }

    @Override
    public void AddMenuItem(String name, String imgPath) {
        var item = new QpMenuItem();
        item.imgPath = imgPath;
        item.name = name;
        gameState.menuItems.add(item);
    }

    @Override
    public void ShowMenu() {
        var inter = gameInterface;
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
        var inter = gameInterface;
        if (inter != null) {
            var windowType = WindowType.values()[type];
            inter.showWindow(windowType, isShow);
        }
    }

    @Override
    public byte[] GetFileContents(String path) {
        return getFileContents(path);
    }

    @Override
    public void ChangeQuestPath(String path) {
        var dir = new File(path);
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
