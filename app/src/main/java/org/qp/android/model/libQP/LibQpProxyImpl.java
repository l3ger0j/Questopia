package org.qp.android.model.libQP;

import static org.qp.android.helpers.utils.FileUtil.createFindDFile;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.FileUtil.getFileContents;
import static org.qp.android.helpers.utils.StringUtil.getStringOrEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
import static org.qp.android.helpers.utils.ThreadUtil.isSameThread;
import static org.qp.android.helpers.utils.ThreadUtil.throwIfNotMainThread;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.qp.android.QuestPlayerApplication;
import org.qp.android.dto.libQP.ActionData;
import org.qp.android.dto.libQP.ErrorData;
import org.qp.android.dto.libQP.GetVarValuesResponse;
import org.qp.android.dto.libQP.ObjectData;
import org.qp.android.helpers.utils.StreamUtil;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.GameContentResolver;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.game.GameInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class LibQpProxyImpl implements LibQpProxy, LibQpCallbacks {
    private final String TAG = this.getClass().getSimpleName();

    private final ReentrantLock libQspLock = new ReentrantLock();
    private final GameState gameState = new GameState();
    private final NativeMethods nativeMethods = new NativeMethods(this);

    private ExecutorService libQspService;
    private volatile Handler libQspHandler;
    private volatile boolean libQspServiceInit;
    private volatile long gameStartTime;
    private volatile long lastMsCountCallTime;
    private GameInterface gameInterface;

    private final Context context;
    private final GameContentResolver gameContentResolver;
    private final HtmlProcessor htmlProcessor;
    private final AudioPlayer audioPlayer;

    private QuestPlayerApplication getApplication() {
        return (QuestPlayerApplication) context.getApplicationContext();
    }

    private DocumentFile getCurGameDir() {
        return gameState.gameDir;
    }

    public LibQpProxyImpl(
            Context context,
            GameContentResolver gameContentResolver,
            HtmlProcessor htmlProcessor,
            AudioPlayer audioPlayer) {
        this.context = context;
        this.gameContentResolver = gameContentResolver;
        this.htmlProcessor = htmlProcessor;
        this.audioPlayer = audioPlayer;
    }

    private void runOnQspThread(final Runnable runnable) {
        throwIfNotMainThread();
        if (libQspService == null) {
            Log.w(TAG ,"Lib service has not been started!");
            return;
        }
        if (!libQspServiceInit) {
            Log.w(TAG ,"Lib service has been started, but not initialized!");
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
        var tempGameFile = documentWrap(gameState.gameFile);

        try (var in = tempGameFile.openInputStream(context)) {
            try (var out = new ByteArrayOutputStream()) {
                if (in != null) {
                    StreamUtil.copy(in, out);
                    gameData = out.toByteArray();
                } else {
                    throw new IOException();
                }
            }
        } catch (IOException ex) {
            Log.e(TAG ,"Failed to load the game world", ex);
            return false;
        }

        var fileName = tempGameFile.getAbsolutePath(context);
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
        if (gameInterface != null) {
            gameInterface.showError(message);
        }
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     * @return <code>true</code> if the configuration has changed, otherwise <code>false</code>
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

    @NonNull
    private ArrayList<QpListItem> getActions() {
        ArrayList<QpListItem> actions = new ArrayList<>();
        var count = nativeMethods.QSPGetActionsCount();
        for (int i = 0; i < count; ++i) {
            var actionData = (ActionData) nativeMethods.QSPGetActionData(i);
            var action = new QpListItem();
            action.pathToImage = actionData.image;
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
            if (objectResult.name.contains("<img")) {
                if (htmlProcessor.hasHTMLTags(objectResult.name)) {
                    var tempPath = htmlProcessor.getSrcDir(objectResult.name);
                    object.pathToImage = String.valueOf(findFileOrDirectory(gameState.gameDir , tempPath));
                } else {
                    object.pathToImage = String.valueOf(findFileOrDirectory(gameState.gameDir , objectResult.name));
                }
            } else {
                object.pathToImage = objectResult.image;
                object.text = gameState.interfaceConfig.useHtml ?
                        htmlProcessor.removeHTMLTags(objectResult.name) : objectResult.name;
            }
            objects.add(object);
        }
        return objects;
    }

    // region LibQpProxy

    public synchronized void start() {
        libQspService = Executors.newSingleThreadExecutor();
        libQspService.submit(() -> {
            nativeMethods.QSPInit();
            if (Looper.myLooper() != null) {
                Looper.myLooper().quitSafely();
            }
            Looper.prepare();
            libQspHandler = new Handler();
            libQspServiceInit = true;
            Looper.loop();
            nativeMethods.QSPDeInit();
        });
    }

    public void stop() {
        throwIfNotMainThread();
        if (libQspService == null) return;
        if (libQspServiceInit) {
            var handler = libQspHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            libQspServiceInit = false;
        } else {
            Log.w(TAG,"Lib service has been started, but not initialized");
        }
        libQspService.shutdown();
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
    public void runGame(final String id,
                        final String title,
                        final DocumentFile dir,
                        final DocumentFile file) {
        runOnQspThread(() -> doRunGame(id, title, dir, file));
    }

    private void doRunGame(final String id,
                           final String title,
                           final DocumentFile dir,
                           final DocumentFile file) {
        gameInterface.doWithCounterDisabled(() -> {
            audioPlayer.closeAllFiles();
            gameState.reset();
            gameState.gameRunning = true;
            gameState.gameId = id;
            gameState.gameTitle = title;
            gameState.gameDir = dir;
            gameState.gameFile = file;
            getApplication().setCurrentGameDir(dir);
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
        runOnQspThread(() ->
                doRunGame(gameState.gameId, gameState.gameTitle,
                        gameState.gameDir, gameState.gameFile));
    }

    @Override
    public void loadGameState(final Uri uri) {
        if (!isSameThread(libQspHandler.getLooper().getThread())) {
            runOnQspThread(() -> loadGameState(uri));
            return;
        }
        final byte[] gameData;
        var resolver = context.getContentResolver();
        try (var in = resolver.openInputStream(uri);
             var out = new ByteArrayOutputStream()) {
            if (in != null) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
            } else {
                throw new IOException("Input is NULL!");
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to load game state", ex);
            return;
        }
        Log.d(TAG , String.valueOf(gameData.length));
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
        var resolver = context.getContentResolver();
        try (var out = resolver.openOutputStream(uri, "w")) {
            if (out != null) {
                out.write(gameData);
            } else {
                throw new IOException("Input is NULL!");
            }
        } catch (IOException ex) {
            Log.e(TAG,"Failed to save the game state", ex);
        }
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
            var input = inter.showInputDialog("userInputTitle");
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
            var input = inter.showExecutorDialog("execStringTitle");
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

    // endregion LibQpProxy

    // region LibQpCallbacks

    @Override
    public void RefreshInt() {
        var request = new RefreshInterfaceRequest();

        var configChanged = loadInterfaceConfiguration();
        if (configChanged) {
            request.interfaceConfigChanged = true;
        }
        if (nativeMethods.QSPIsMainDescChanged()) {
            if (gameState.mainDesc != null) {
                if (!gameState.mainDesc.equals(nativeMethods.QSPGetMainDesc())) {
                    gameState.mainDesc = nativeMethods.QSPGetMainDesc();
                    request.mainDescChanged = true;
                }
            } else {
                gameState.mainDesc = nativeMethods.QSPGetMainDesc();
                request.mainDescChanged = true;
            }
        }
        if (nativeMethods.QSPIsActionsChanged()) {
            if (gameState.actions != null) {
                if (gameState.actions != getActions()) {
                    gameState.actions = getActions();
                    request.actionsChanged = true;
                }
            } else {
                gameState.actions = getActions();
                request.actionsChanged = true;
            }
        }
        if (nativeMethods.QSPIsObjectsChanged()) {
            if (gameState.objects != null) {
                if (gameState.objects != getObjects()) {
                    gameState.objects = getObjects();
                    request.objectsChanged = true;
                }
            } else {
                gameState.objects = getObjects();
                request.objectsChanged = true;
            }
        }
        if (nativeMethods.QSPIsVarsDescChanged()) {
            if (gameState.varsDesc != null) {
                if (!gameState.varsDesc.equals(nativeMethods.QSPGetVarsDesc())) {
                    gameState.varsDesc = nativeMethods.QSPGetVarsDesc();
                    request.varsDescChanged = true;
                }
            } else {
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
        if (inter == null) return;
        if (filename != null) {
            try {
                var uri = getApplication().fromFullPath(filename).getUri();
                inter.doWithCounterDisabled(() -> loadGameState(uri));
            } catch (Exception e) {
                Log.e(TAG , "Error: ", e);
            }
        } else {
            Log.e(TAG , "Save file not found");
            inter.showLoadGamePopup();
        }
    }

    @Override
    public void SaveGame(String filename) {
        var inter = gameInterface;
        if (inter == null) return;
        if (filename != null) {
            var file = new File(filename);
            var curGameDir = gameState.gameDir;
            var saveFile =  createFindDFile(curGameDir , MimeType.TEXT , file.getName());
            if (saveFile != null) {
                saveGameState(saveFile.getUri());
            } else {
                Log.e(TAG , "File not created");
            }
        } else {
            inter.showSaveGamePopup();
        }
    }

    @Override
    public String InputBox(String prompt) {
        return gameInterface != null ? gameInterface.showInputDialog(prompt) : null;
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

    // TODO: 09.10.2023 NEEED TO ADD ERROR CATCHER
    @Override
    public byte[] GetFileContents(String path) {
        var name = gameState.gameDir.getName();
        if (name == null) return null;
        var targetFileUri = getApplication().fromFullPath(path).getUri();
        return getFileContents(context , targetFileUri);
    }

    @Override
    public void ChangeQuestPath(String path) {
        var newGameDir = getApplication().fromFullPath(path);
        if (!newGameDir.exists()) {
            Log.e(TAG,"Game directory not found: " + path);
            return;
        }
        if (!Objects.equals(getCurGameDir() , newGameDir)) {
            gameState.gameDir = newGameDir;
            getApplication().setCurrentGameDir(newGameDir);
        }
    }

    // endregion LibQpCallbacks
}
