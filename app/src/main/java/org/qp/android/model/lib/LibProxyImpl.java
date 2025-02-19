package org.qp.android.model.lib;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.fromFullPath;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.getFileContents;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.FileUtil.writeFileContents;
import static org.qp.android.helpers.utils.PathUtil.getFilename;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;
import static org.qp.android.helpers.utils.StringUtil.getStringOrEmpty;
import static org.qp.android.helpers.utils.StringUtil.isNotEmpty;
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
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.libndkqsp.jni.NDKLib;
import org.qp.android.QuestopiaApplication;
import org.qp.android.model.service.AudioPlayer;
import org.qp.android.model.service.HtmlProcessor;
import org.qp.android.ui.game.GameInterface;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class LibProxyImpl extends NDKLib implements LibIProxy {
    private final String TAG = this.getClass().getSimpleName();

    private final ReentrantLock libLock = new ReentrantLock();
    private final LibGameState gameState = new LibGameState();

    private Thread libThread;
    private volatile Handler libHandler;
    private volatile boolean libThreadInit;
    private volatile long gameStartTime;
    private volatile long lastMsCountCallTime;
    private GameInterface gameInterface;

    private final WeakReference<Context> context;

    private QuestopiaApplication getApplication() {
        return (QuestopiaApplication) context.get();
    }

    private DocumentFile getCurGameDir() {
        return gameState.gameDir;
    }

    private HtmlProcessor getHtmlProcessor() {
        return getApplication().getHtmlProcessor();
    }

    public AudioPlayer getAudioPlayer() {
        return getApplication().getAudioPlayer();
    }

    public LibProxyImpl(Context context) {
        this.context = new WeakReference<>(context);
    }

    private void runOnQspThread(final Runnable runnable) {
        throwIfNotMainThread();
        if (libThread == null) {
            Log.w(TAG ,"Lib thread has not been started!");
            return;
        }
        if (!libThreadInit) {
            Log.w(TAG ,"Lib thread has been started, but not initialized!");
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
        var gameFileUri = gameState.gameFile.getUri();
        var gameFileFullPath = documentWrap(gameState.gameFile).getAbsolutePath(context.get());
        final var gameData = getFileContents(context.get(), gameFileUri);
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
        Log.e(TAG,message);
        if (gameInterface != null) {
            gameInterface.showErrorDialog(message);
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

        var htmlResult = (VarValResp) QSPGetVarValues("USEHTML", 0);
        if (htmlResult.isSuccess()) {
            boolean useHtml = htmlResult.intValue() != 0;
            if (config.useHtml != useHtml) {
                config.useHtml = useHtml;
                changed = true;
            }
        }
        var fSizeResult = (VarValResp) QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.isSuccess() && config.fontSize != fSizeResult.intValue()) {
            config.fontSize = fSizeResult.intValue();
            changed = true;
        }
        var bColorResult = (VarValResp) QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.isSuccess() && config.backColor != bColorResult.intValue()) {
            config.backColor = bColorResult.intValue();
            changed = true;
        }
        var fColorResult = (VarValResp) QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.isSuccess() && config.fontColor != fColorResult.intValue()) {
            config.fontColor = fColorResult.intValue();
            changed = true;
        }
        var lColorResult = (VarValResp) QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.isSuccess() && config.linkColor != lColorResult.intValue()) {
            config.linkColor = lColorResult.intValue();
            changed = true;
        }

        return changed;
    }

    @NonNull
    private ArrayList<ListItem> getActionsList() {
        var actions = new ArrayList<ListItem>();
        var currGameDir = getCurGameDir();

        for (var element : QSPGetActionData()) {
            var tempImagePath = element.image() == null ? "" : element.image();
            var tempText = element.text() == null ? "" : element.text();

            if (isNotEmptyOrBlank(tempImagePath)) {
                var tempPath = normalizeContentPath(getFilename(tempImagePath));
                var fileFromPath = fromRelPath(context.get(), tempPath, currGameDir);
                if (fileFromPath != null) {
                    tempImagePath = String.valueOf(fileFromPath.getUri());
                }
            }

            actions.add(new ListItem(tempImagePath, tempText));
        }

        return actions;
    }

    @NonNull
    private ArrayList<ListItem> getObjectsList() {
        var objects = new ArrayList<ListItem>();
        var currGameDir = getCurGameDir();

        for (var element : QSPGetObjectData()) {
            var tempImagePath = element.image() == null ? "" : element.image();
            var tempText = element.text() == null ? "" : element.text();

            if (tempText.contains("<img")) {
                if (getHtmlProcessor().isContainsHtmlTags(tempText)) {
                    var tempPath = getHtmlProcessor().getSrcDir(tempText);
                    var fileFromPath = fromRelPath(context.get(), tempPath, currGameDir);
                    tempImagePath = String.valueOf(fileFromPath);
                } else {
                    var fileFromPath = fromRelPath(context.get(), tempText, currGameDir);
                    tempImagePath = String.valueOf(fileFromPath);
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
                    Log.e(TAG , "lib thread has stopped exceptionally" , t);
                    Thread.currentThread().interrupt();
                }
            }
        } , "libQSP");
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
            Log.w(TAG,"libqsp thread has been started, but not initialized");
        }
        libThread.interrupt();
    }

    public void enableDebugMode (boolean isDebug) {
        runOnQspThread(() -> QSPEnableDebugMode(isDebug));
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
            getAudioPlayer().closeAllFiles();
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
            if (!QSPRestartGame(true)) {
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
        if (!isSameThread(libHandler.getLooper().getThread())) {
            runOnQspThread(() -> loadGameState(uri));
            return;
        }
        final var gameData = getFileContents(context.get() , uri);
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
        writeFileContents(context.get() , uri , gameData);
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
            if (gameState.mainDesc != null) {
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
            if (gameState.actionsList != null) {
                if (gameState.actionsList != getActionsList()) {
                    gameState.actionsList = getActionsList();
                    request.isActionsChanged = true;
                }
            } else {
                gameState.actionsList = getActionsList();
                request.isActionsChanged = true;
            }
        }
        if (QSPIsObjectsChanged()) {
            if (gameState.objectsList != null) {
                if (gameState.objectsList != getObjectsList()) {
                    gameState.objectsList = getObjectsList();
                    request.isObjectsChanged = true;
                }
            } else {
                gameState.objectsList = getObjectsList();
                request.isObjectsChanged = true;
            }
        }
        if (QSPIsVarsDescChanged()) {
            if (gameState.varsDesc != null) {
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
    public void ShowPicture(String path) {
        var inter = gameInterface;
        if (inter == null) return;

        if (isNotEmpty(path)) {
            var picFile = fromRelPath(context.get(), path, getCurGameDir());
            if (!isWritableFile(context.get(), picFile)) return;
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
        if (isNotEmpty(path)) {
            getAudioPlayer().closeFile(path);
        } else {
            getAudioPlayer().closeAllFiles();
        }
    }

    @Override
    public void OpenGame(String filename) {
        var inter = gameInterface;
        if (inter == null) return;

        if (filename == null) {
            inter.showLoadGamePopup();
        } else {
            try {
                var saveFile = fromFullPath(context.get(), filename);
                if (saveFile == null) {
                    Log.e(TAG , "Save file not found");
                    return;
                }
                var saveFileUri = saveFile.getUri();
                inter.doWithCounterDisabled(() -> loadGameState(saveFileUri));
            } catch (Exception e) {
                Log.e(TAG , "Error: ", e);
            }
        }
    }

    @Override
    public void SaveGame(String filename) {
        if (filename == null) {
            var inter = gameInterface;
            if (inter == null) return;
            inter.showSaveGamePopup();
        } else {
            var file = new File(filename);
            var saveFile = findOrCreateFile(context.get(), getCurGameDir(), file.getName(), MimeType.TEXT);
            if (saveFile != null) {
                saveGameState(saveFile.getUri());
            } else {
                Log.e(TAG , "Error access dir");
            }
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
        gameState.menuItemsList.add(new ListItem(name, imgPath));
    }

    @Override
    public void ShowMenu() {
        var inter = gameInterface;
        if (inter == null) return;
        int result = inter.showMenu();
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
            Log.e(TAG,"Wait failed", ex);
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
        var targetFile = fromRelPath(context.get(), path, getCurGameDir());
        if (targetFile == null) return null;
        var targetFileUri = targetFile.getUri();
        return getFileContents(context.get(), targetFileUri);
    }

    @Override
    public void ChangeQuestPath(String path) {
        var newGameDir = fromFullPath(context.get(), path);
        if (newGameDir == null || !newGameDir.exists()) {
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
