package org.qp.android.model.lib;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.fromFullPath;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.getFileContents;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.FileUtil.writeFileContents;
import static org.qp.android.helpers.utils.PathUtil.getFilename;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;
import static org.qp.android.helpers.utils.StringUtil.getStringOrEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;
import static org.qp.android.helpers.utils.ThreadUtil.isSameThread;
import static org.qp.android.helpers.utils.ThreadUtil.throwIfNotMainThread;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;

import org.libndkqsp.jni.NDKLib;
import org.qp.android.QuestopiaApplication;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.game.GameInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class LibProxyImpl extends NDKLib implements LibIProxy {
    private final String TAG = this.getClass().getSimpleName();

    private final ReentrantLock libLock = new ReentrantLock();
    private final LibGameState gameState = new LibGameState();
    private final Context context;
    private Thread libThread;
    private volatile Handler libHandler;
    private volatile boolean libThreadInit;
    private volatile long gameStartTime;
    private volatile long lastMsCountCallTime;
    private GameInterface gameInterface;

    public LibProxyImpl(Context context) {
        this.context = context;
    }

    private QuestopiaApplication getApplication() {
        return (QuestopiaApplication) context;
    }

    @Nullable
    private DocumentFile getCurGameDir() {
        var file = DocumentFileCompat.fromUri(context, gameState.gameDirUri);
        if (!isWritableDir(context, file)) return null;
        return file;
    }

    private HtmlProcessor getHtmlProcessor() {
        return getApplication().getHtmlProcessor();
    }

    public AudioPlayer getAudioPlayer() {
        return getApplication().audioPlayer
                .setCurGameDir(getCurGameDir());
    }

    private void runOnQspThread(final Runnable runnable) {
        throwIfNotMainThread();
        if (libThread == null) {
            Log.w(TAG, "Lib thread has not been started!");
            return;
        }
        if (!libThreadInit) {
            Log.w(TAG, "Lib thread has been started, but not initialized!");
            return;
        }
        var mLibHandler = libHandler;
        if (mLibHandler == null) return;
        mLibHandler.post(() -> {
            libLock.lock();
            try {
                runnable.run();
            } finally {
                libLock.unlock();
            }
        });
    }

    private boolean loadGameWorld() {
        final var gameFileUri = gameState.gameFileUri;
        var gameFile = DocumentFileCompat.fromUri(context, gameState.gameFileUri);
        if (!isWritableFile(context, gameFile)) return false;
        var gameFileFullPath = documentWrap(gameFile).getAbsolutePath(context);
        if (!isNotEmptyOrBlank(gameFileFullPath)) return false;
        final var gameData = getFileContents(context, gameFileUri);
        if (gameData == null) return false;
        if (!QSPLoadGameWorldFromData(gameData, gameFileFullPath)) {
            showLastQspError();
            return false;
        }
        return true;
    }

    private void showLastQspError() {
        var errorData = QSPGetLastErrorData();
        var locName = getStringOrEmpty(errorData.locName());
        var desc = getStringOrEmpty(QSPGetErrorDesc(errorData.errorNum()));
        final var message = String.format(
                Locale.getDefault(),
                "Location: %s\nAction: %d\nLine: %d\nError number: %d\nDescription: %s",
                locName,
                errorData.index(),
                errorData.line(),
                errorData.errorNum(),
                desc);
        Log.e(TAG, message);
        if (gameInterface == null) return;
        gameInterface.showErrorDialog(message);
    }

    /**
     * Loads the interface configuration - using HTML, font and colors - from the library.
     *
     * @return <code>true</code> if the configuration has changed, otherwise <code>false</code>
     */
    private boolean loadInterfaceConfiguration() {
        final var config = gameState.interfaceConfig;
        var changed = false;

        final var htmlResult = (VarValResp) QSPGetVarValues("USEHTML", 0);
        if (htmlResult.isSuccess()) {
            boolean useHtml = htmlResult.intValue() != 0;
            if (config.useHtml != useHtml) {
                config.useHtml = useHtml;
                changed = true;
            }
        }

        final var fSizeResult = (VarValResp) QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.isSuccess() && config.fontSize != fSizeResult.intValue()) {
            config.fontSize = fSizeResult.intValue();
            changed = true;
        }

        final var bColorResult = (VarValResp) QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.isSuccess() && config.backColor != bColorResult.intValue()) {
            config.backColor = bColorResult.intValue();
            changed = true;
        }

        final var fColorResult = (VarValResp) QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.isSuccess() && config.fontColor != fColorResult.intValue()) {
            config.fontColor = fColorResult.intValue();
            changed = true;
        }

        final var lColorResult = (VarValResp) QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.isSuccess() && config.linkColor != lColorResult.intValue()) {
            config.linkColor = lColorResult.intValue();
            changed = true;
        }

        return changed;
    }

    @NonNull
    private List<ListItem> getActionsList() {
        var gameDir = getCurGameDir();
        if (!isWritableDir(context, gameDir)) return Collections.emptyList();
        var actions = new ArrayList<ListItem>();

        for (var element : QSPGetActionData()) {
            var tempImagePath = element.image() == null ? "" : element.image();
            var tempText = element.text() == null ? "" : element.text();

            if (isNotEmptyOrBlank(tempImagePath)) {
                var tempPath = normalizeContentPath(getFilename(tempImagePath));
                var fileFromPath = fromRelPath(context, tempPath, gameDir);
                if (isWritableFile(context, fileFromPath)) {
                    tempImagePath = String.valueOf(fileFromPath.getUri());
                }
            }

            actions.add(new ListItem(tempImagePath, tempText));
        }

        return actions;
    }

    @NonNull
    private List<ListItem> getObjectsList() {
        var gameDir = getCurGameDir();
        if (!isWritableDir(context, gameDir)) return Collections.emptyList();
        var objects = new ArrayList<ListItem>();

        for (var element : QSPGetObjectData()) {
            var tempImagePath = element.image() == null ? "" : element.image();
            var tempText = element.text() == null ? "" : element.text();

            if (tempText.contains("<img")) {
                if (getHtmlProcessor().isContainsHtmlTags(tempText)) {
                    var tempPath = getHtmlProcessor().getSrcDir(tempText);
                    var fileFromPath = fromRelPath(context, tempPath, gameDir);
                    if (isWritableFile(context, fileFromPath)) {
                        tempImagePath = String.valueOf(fileFromPath.getUri());
                    }
                } else {
                    var fileFromPath = fromRelPath(context, tempText, gameDir);
                    if (isWritableFile(context, fileFromPath)) {
                        tempImagePath = String.valueOf(fileFromPath.getUri());
                    }
                }
            }

            objects.add(new ListItem(tempImagePath, tempText));
        }

        return objects;
    }

    // region LibQpProxy

    public void startLibThread() {
        libThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    QSPInit();
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    libHandler = new Handler(Looper.myLooper());
                    libThreadInit = true;
                    Looper.loop();
                    QSPDeInit();
                } catch (Throwable t) {
                    Log.e(TAG, "lib thread has stopped exceptionally", t);
                    Thread.currentThread().interrupt();
                }
            }
        }, "libQSP");
        libThread.start();
    }

    public void stopLibThread() {
        throwIfNotMainThread();
        if (libThread == null) return;
        if (libThreadInit) {
            var handler = libHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            libThreadInit = false;
        } else {
            Log.w(TAG, "libqsp thread has been started, but not initialized");
        }
        libThread.interrupt();
    }

    public void enableDebugMode(boolean isDebug) {
        runOnQspThread(() -> enableDebugMode(isDebug));
    }

    @Override
    public void runGame(final long id,
                        final String title,
                        final Uri dir,
                        final Uri file) {
        runOnQspThread(() -> doRunGame(id, title, dir, file));
    }

    private void doRunGame(final long id,
                           final String title,
                           final Uri dir,
                           final Uri file) {
        gameInterface.doWithCounterDisabled(() -> {
            gameState.reset();
            gameState.gameRunning = true;
            gameState.gameId = id;
            gameState.gameTitle = title;
            gameState.gameDirUri = dir;
            gameState.gameFileUri = file;
            getAudioPlayer().closeAllFiles();
            if (!loadGameWorld()) return;
            gameStartTime = SystemClock.elapsedRealtime();
            lastMsCountCallTime = 0;
            if (!QSPRestartGame(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void restartGame() {
        runOnQspThread(() ->
                doRunGame(gameState.gameId, gameState.gameTitle,
                        gameState.gameDirUri, gameState.gameFileUri));
    }

    @Override
    public void loadGameState(final Uri uri) {
        if (!isSameThread(libHandler.getLooper().getThread())) {
            runOnQspThread(() -> loadGameState(uri));
            return;
        }
        final var gameData = getFileContents(context, uri);
        if (gameData == null) return;
        if (!QSPOpenSavedGameFromData(gameData, gameData.length, true)) {
            showLastQspError();
        }
    }

    @Override
    public void saveGameState(final Uri uri) {
        if (!isSameThread(libHandler.getLooper().getThread())) {
            runOnQspThread(() -> saveGameState(uri));
            return;
        }
        final var gameData = QSPSaveGameAsData(false);
        if (gameData == null) return;
        writeFileContents(context, uri, gameData);
    }

    @Override
    public void onActionClicked(final int index) {
        runOnQspThread(() -> {
            if (!QSPSetSelActionIndex(index, false)) {
                showLastQspError();
            }
            if (!QSPExecuteSelActionCode(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void onObjectSelected(final int index) {
        runOnQspThread(() -> {
            if (!QSPSetSelObjectIndex(index, true)) {
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
            if (!isNotEmptyOrBlank(input)) return;
            QSPSetInputStrText(input);
            if (!QSPExecUserInput(true)) {
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
            if (!isNotEmptyOrBlank(input)) return;
            if (!QSPExecString(input, true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void execute(final String code) {
        runOnQspThread(() -> {
            if (!QSPExecString(code, true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void executeCounter() {
        if (libLock.isLocked()) return;
        runOnQspThread(() -> {
            if (!QSPExecCounter(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public LibGameState getGameState() {
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
        var request = new LibRefIRequest();
        var configChanged = loadInterfaceConfiguration();

        if (configChanged) {
            request.isIConfigChanged = true;
        }
        if (QSPIsMainDescChanged()) {
            if (isNotEmptyOrBlank(gameState.mainDesc)) {
                if (!gameState.mainDesc.equals(QSPGetMainDesc())) {
                    gameState.mainDesc = QSPGetMainDesc();
                    request.isMainDescChanged = true;
                }
            } else {
                gameState.mainDesc = QSPGetMainDesc();
                request.isMainDescChanged = true;
            }
        }
        if (QSPIsActionsChanged()) {
            if (gameState.actionsList.isEmpty()) {
                gameState.actionsList = getActionsList();
                request.isActionsChanged = true;
            } else {
                if (gameState.actionsList != getActionsList()) {
                    gameState.actionsList = getActionsList();
                    request.isActionsChanged = true;
                }
            }
        }
        if (QSPIsObjectsChanged()) {
            if (gameState.objectsList.isEmpty()) {
                gameState.objectsList = getObjectsList();
                request.isObjectsChanged = true;
            } else {
                if (gameState.objectsList != getObjectsList()) {
                    gameState.objectsList = getObjectsList();
                    request.isObjectsChanged = true;
                }
            }
        }
        if (QSPIsVarsDescChanged()) {
            if (isNotEmptyOrBlank(gameState.varsDesc)) {
                if (!gameState.varsDesc.equals(QSPGetVarsDesc())) {
                    gameState.varsDesc = QSPGetVarsDesc();
                    request.isVarsDescChanged = true;
                }
            } else {
                gameState.varsDesc = QSPGetVarsDesc();
                request.isVarsDescChanged = true;
            }
        }

        var inter = gameInterface;
        if (inter != null) {
            inter.refresh(request);
        }
    }

    @Override
    public void ShowPicture(String file) {
        var gameDir = getCurGameDir();
        if (!isWritableDir(context, gameDir)) return;

        var inter = gameInterface;
        if (inter == null) return;

        if (isNotEmptyOrBlank(file)) {
            var picFile = fromRelPath(context, file, gameDir);
            if (!isWritableFile(context, picFile)) return;
            inter.showPicture(String.valueOf(picFile.getUri()));
        }
    }

    @Override
    public void SetTimer(int msecs) {
        var inter = gameInterface;
        if (inter == null) return;

        inter.setCounterInterval(msecs);
    }

    @Override
    public void ShowMessage(String message) {
        var inter = gameInterface;
        if (inter == null) return;

        inter.showMessage(message);
    }

    @Override
    public void PlayFile(String path, int volume) {
        if (!isNotEmptyOrBlank(path)) return;

        getAudioPlayer().playFile(path, volume);
    }

    @Override
    public boolean IsPlayingFile(final String path) {
        return isNotEmptyOrBlank(path) && getAudioPlayer().isPlayingFile(path);
    }

    @Override
    public void CloseFile(String path) {
        if (isNotEmptyOrBlank(path)) {
            getAudioPlayer().closeFile(path);
        } else {
            getAudioPlayer().closeAllFiles();
        }
    }

    @Override
    public void OpenGame(String filename) {
        var inter = gameInterface;
        if (inter == null) return;
        var currGameDir = getCurGameDir();
        if (currGameDir == null) return;

        if (!isNotEmptyOrBlank(filename)) {
            inter.showLoadGamePopup();
        } else {
            try {
                var saveFile = fromRelPath(context, filename, currGameDir);
                if (!isWritableFile(context, saveFile)) {
                    saveFile = fromFullPath(context, filename);
                    if (!isWritableFile(context, saveFile)) {
                        Log.e(TAG, "Save file not found");
                        return;
                    }
                }
                var saveFileUri = saveFile.getUri();
                inter.doWithCounterDisabled(() -> loadGameState(saveFileUri));
            } catch (Exception e) {
                Log.e(TAG, "Error: ", e);
            }
        }
    }

    @Override
    public void SaveGame(String filename) {
        var gameDir = getCurGameDir();
        if (!isWritableDir(context, gameDir)) return;

        if (!isNotEmptyOrBlank(filename)) {
            var inter = gameInterface;
            if (inter == null) return;
            inter.showSaveGamePopup();
        } else {
            var saveFile = findOrCreateFile(context, gameDir,
                    new File(filename).getName(),
                    MimeType.TEXT
            );
            if (isWritableFile(context, saveFile)) {
                saveGameState(saveFile.getUri());
            } else {
                Log.e(TAG, "Error access dir");
            }
        }
    }

    @Override
    public String InputBox(String prompt) {
        var inter = gameInterface;
        if (inter == null) return "";

        return isNotEmptyOrBlank(prompt) ? gameInterface.showInputDialog(prompt) : "";
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
        var item = new NDKLib.ListItem(imgPath, name);
        gameState.menuItemsList.add(item);
    }

    @Override
    public void ShowMenu() {
        var inter = gameInterface;
        if (inter == null) return;
        var result = inter.showMenu();
        if (result != -1) {
            QSPSelectMenuItem(result);
        }
    }

    @Override
    public void DeleteMenu() {
        gameState.menuItemsList.clear();
    }

    @Override
    public void Wait(int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException ex) {
            Log.e(TAG, "Wait failed", ex);
        }
    }

    @Override
    public void ShowWindow(int type, boolean isShow) {
        var inter = gameInterface;
        if (inter == null) return;
        var windowType = LibWindowType.values()[type];
        inter.showWindow(windowType, isShow);
    }

    @Override
    public byte[] GetFileContents(String path) {
        var gameDir = getCurGameDir();
        if (gameDir == null) return null;

        var targetFile = fromRelPath(context, path, gameDir);
        if (!isWritableFile(context, targetFile)) {
            targetFile = fromFullPath(context, path);
            if (!isWritableFile(context, targetFile)) return null;
        }
        var targetFileUri = targetFile.getUri();
        return getFileContents(context, targetFileUri);
    }

    @Override
    public void ChangeQuestPath(String path) {
        var oldGameDir = getCurGameDir();
        if (oldGameDir == null) return;

        var newGameDir = fromFullPath(context, path);
        if (!isWritableFile(context, newGameDir)) {
            return;
        }

        var currGameDirUri = oldGameDir.getUri();
        var newGameDirUri = newGameDir.getUri();
        if (!Objects.equals(currGameDirUri, newGameDirUri)) {
            gameState.gameDirUri = newGameDirUri;
        }
    }

    // endregion LibQpCallbacks
}
