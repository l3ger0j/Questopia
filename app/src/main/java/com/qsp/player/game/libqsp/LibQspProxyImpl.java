package com.qsp.player.game.libqsp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.preference.PreferenceManager;

import com.qsp.player.R;
import com.qsp.player.game.AudioPlayer;
import com.qsp.player.game.HtmlProcessor;
import com.qsp.player.game.ImageProvider;
import com.qsp.player.game.PlayerView;
import com.qsp.player.game.PlayerViewState;
import com.qsp.player.game.QspListItem;
import com.qsp.player.game.QspMenuItem;
import com.qsp.player.game.WindowType;
import com.qsp.player.game.libqsp.dto.ActionData;
import com.qsp.player.game.libqsp.dto.ErrorData;
import com.qsp.player.game.libqsp.dto.GetVarValuesResponse;
import com.qsp.player.game.libqsp.dto.ObjectData;
import com.qsp.player.util.StreamUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import static com.qsp.player.util.FileUtil.findFileOrDirectory;
import static com.qsp.player.util.FileUtil.getFileContents;
import static com.qsp.player.util.FileUtil.getOrCreateDirectory;
import static com.qsp.player.util.FileUtil.normalizePath;
import static com.qsp.player.util.StringUtil.getStringOrEmpty;
import static com.qsp.player.util.ThreadUtil.throwIfThreadIsNotMain;

public class LibQspProxyImpl implements LibQspProxy, LibQspCallbacks {
    private static final Logger logger = LoggerFactory.getLogger(LibQspProxyImpl.class);

    private final Handler counterHandler = new Handler();
    private final ReentrantLock libQspLock = new ReentrantLock();
    private final PlayerViewState viewState = new PlayerViewState();
    private final AudioPlayer audioPlayer = new AudioPlayer();
    private final NativeMethods nativeMethods = new NativeMethods(this);

    private final Context context;
    private final ImageProvider imageProvider;
    private final HtmlProcessor htmlProcessor;

    private final Runnable counterTask = new Runnable() {
        @Override
        public void run() {
            // Обрабатывать локацию-счётчик, только если поток библиотеки сейчас ничего не делает
            if (!libQspLock.isLocked()) {
                runOnQspThread(() -> {
                    if (!nativeMethods.QSPExecCounter(true)) {
                        showLastQspError();
                    }
                });
            }
            counterHandler.postDelayed(this, timerInterval);
        }
    };

    private SharedPreferences settings;
    private Thread libQspThread;
    private volatile Handler libQspHandler;
    private volatile boolean libQspThreadInited;
    private volatile long gameStartTime;
    private volatile long lastMsCountCallTime;
    private volatile int timerInterval;
    private PlayerView playerView;

    public LibQspProxyImpl(Context context, ImageProvider imageProvider, HtmlProcessor htmlProcessor) {
        this.context = context;
        this.imageProvider = imageProvider;
        this.htmlProcessor = htmlProcessor;
    }

    public void init() {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void start() {
        libQspThread = new Thread("libqsp") {
            @Override
            public void run() {
                try {
                    nativeMethods.QSPInit();
                    Looper.prepare();
                    libQspHandler = new Handler();
                    libQspThreadInited = true;

                    Looper.loop();

                    nativeMethods.QSPDeInit();
                } catch (Throwable t) {
                    logger.error("libqsp thread has stopped exceptionally", t);
                }
            }
        };
        libQspThread.start();
    }

    private void runOnQspThread(final Runnable runnable) {
        throwIfThreadIsNotMain();

        // Callback с блокировкой, который необходимо выполнить
        final Runnable runnableWithLock = () -> {
            libQspLock.lock();
            try {
                runnable.run();
            } finally {
                libQspLock.unlock();
            }
        };

        // Выйти если поток был запущен, но не был проинициализирован
        if (!libQspThreadInited) {
            logger.warn("libqsp thread has been started, but not initialized");
            return;
        }

        Handler handler = libQspHandler;
        if (handler != null) {
            handler.post(runnableWithLock);
        }
    }

    public void stop() {
        audioPlayer.close();
        stopLibQspThread();
    }

    private void stopLibQspThread() {
        throwIfThreadIsNotMain();

        if (libQspThread == null) return;

        if (libQspThreadInited) {
            Handler handler = libQspHandler;
            if (handler != null) {
                handler.getLooper().quitSafely();
            }
            libQspThreadInited = false;
        } else {
            logger.warn("libqsp thread has been started, but not initialized");
        }
        libQspThread = null;
    }

    private void showLastQspError() {
        ErrorData errorData = (ErrorData) nativeMethods.QSPGetLastErrorData();
        String locName = getStringOrEmpty(errorData.getLocName());
        String desc = getStringOrEmpty(nativeMethods.QSPGetErrorDesc(errorData.getErrorNum()));

        final String message = String.format(
                Locale.getDefault(),
                "Location: %s\nAction: %d\nLine: %d\nError number: %d\nDescription: %s",
                locName,
                errorData.getIndex(),
                errorData.getLine(),
                errorData.getErrorNum(),
                desc);

        logger.error(message);

        PlayerView view = playerView;
        if (view != null) {
            playerView.showError(message);
        }
    }

    private boolean loadGameWorld() {
        byte[] gameData;
        try (FileInputStream in = new FileInputStream(viewState.getGameFile())) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
            }
        } catch (IOException e) {
            logger.error("Failed to load the game world", e);
            return false;
        }
        String fileName = viewState.getGameFile().getAbsolutePath();
        if (!nativeMethods.QSPLoadGameWorldFromData(gameData, gameData.length, fileName)) {
            showLastQspError();
            return false;
        }

        return true;
    }

    private boolean loadUIConfiguration() {
        boolean changed = false;

        GetVarValuesResponse htmlResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("USEHTML", 0);
        if (htmlResult.isSuccess()) {
            boolean useHtml = htmlResult.getIntValue() != 0;
            if (viewState.isUseHtml() != useHtml) {
                viewState.setUseHtml(useHtml);
                changed = true;
            }
        }

        GetVarValuesResponse fSizeResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FSIZE", 0);
        if (fSizeResult.isSuccess() && viewState.getFontSize() != fSizeResult.getIntValue()) {
            viewState.setFontSize(fSizeResult.getIntValue());
            changed = true;
        }

        GetVarValuesResponse bColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("BCOLOR", 0);
        if (bColorResult.isSuccess() && viewState.getBackColor() != bColorResult.getIntValue()) {
            viewState.setBackColor(bColorResult.getIntValue());
            changed = true;
        }

        GetVarValuesResponse fColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("FCOLOR", 0);
        if (fColorResult.isSuccess() && viewState.getFontColor() != fColorResult.getIntValue()) {
            viewState.setFontColor(fColorResult.getIntValue());
            changed = true;
        }

        GetVarValuesResponse lColorResult = (GetVarValuesResponse) nativeMethods.QSPGetVarValues("LCOLOR", 0);
        if (lColorResult.isSuccess() && viewState.getLinkColor() != lColorResult.getIntValue()) {
            viewState.setLinkColor(lColorResult.getIntValue());
            changed = true;
        }

        return changed;
    }

    private void loadActions() {
        ArrayList<QspListItem> actions = new ArrayList<>();
        int count = nativeMethods.QSPGetActionsCount();
        for (int i = 0; i < count; ++i) {
            ActionData actionData = (ActionData) nativeMethods.QSPGetActionData(i);
            QspListItem action = new QspListItem();
            action.icon = imageProvider.getDrawable(normalizePath(actionData.getImage()));
            action.text = viewState.isUseHtml() ? htmlProcessor.removeHtmlTags(actionData.getName()) : actionData.getName();
            actions.add(action);
        }
        viewState.setActions(actions);
    }

    private void loadObjects() {
        ArrayList<QspListItem> objects = new ArrayList<>();
        int count = nativeMethods.QSPGetObjectsCount();
        for (int i = 0; i < count; i++) {
            ObjectData objectResult = (ObjectData) nativeMethods.QSPGetObjectData(i);
            QspListItem object = new QspListItem();
            object.icon = imageProvider.getDrawable(normalizePath(objectResult.getImage()));
            object.text = viewState.isUseHtml() ? htmlProcessor.removeHtmlTags(objectResult.getName()) : objectResult.getName();
            objects.add(object);
        }
        viewState.setObjects(objects);
    }

    // region LibQspProxy

    @Override
    public PlayerViewState getViewState() {
        return viewState;
    }

    @Override
    public void setPlayerView(PlayerView view) {
        playerView = view;
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
        final PlayerView view = playerView;
        if (view == null) {
            return;
        }
        runOnQspThread(() -> {
            String input = view.showInputBox(context.getString(R.string.userInput));
            nativeMethods.QSPSetInputStrText(input);

            if (!nativeMethods.QSPExecUserInput(true)) {
                showLastQspError();
            }
        });
    }

    @Override
    public void runGame(final String id, final String title, final File dir, final File file) {
        runOnQspThread(() -> doRunGame(id, title, dir, file));
    }

    private void doRunGame(final String id, final String title, final File dir, final File file) {
        counterHandler.removeCallbacks(counterTask);
        audioPlayer.closeAllFiles();

        viewState.reset();
        viewState.setGameRunning(true);
        viewState.setGameId(id);
        viewState.setGameTitle(title);
        viewState.setGameDir(dir);
        viewState.setGameFile(file);

        imageProvider.invalidateCache();

        if (!loadGameWorld()) {
            return;
        }

        gameStartTime = SystemClock.elapsedRealtime();
        lastMsCountCallTime = 0;
        timerInterval = 500;

        if (!nativeMethods.QSPRestartGame(true)) {
            showLastQspError();
        }

        counterHandler.postDelayed(counterTask, timerInterval);
    }

    @Override
    public void restartGame() {
        runOnQspThread(() -> {
            PlayerViewState state = viewState;
            doRunGame(state.getGameId(), state.getGameTitle(), state.getGameDir(), state.getGameFile());
        });
    }

    @Override
    public void resumeGame() {
        audioPlayer.setSoundEnabled(settings.getBoolean("sound", true));
        audioPlayer.resume();
        counterHandler.postDelayed(counterTask, timerInterval);
    }

    @Override
    public void pauseGame() {
        counterHandler.removeCallbacks(counterTask);
        audioPlayer.pause();
    }

    @Override
    public void loadGameState(final Uri uri) {
        if (Thread.currentThread() != libQspHandler.getLooper().getThread()) {
            runOnQspThread(() -> loadGameState(uri));
            return;
        }

        final byte[] gameData;

        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                StreamUtil.copy(in, out);
                gameData = out.toByteArray();
            }
        } catch (IOException e) {
            logger.error("Failed to load game state", e);
            return;
        }

        counterHandler.removeCallbacks(counterTask);

        if (!nativeMethods.QSPOpenSavedGameFromData(gameData, gameData.length, true)) {
            showLastQspError();
        }

        counterHandler.postDelayed(counterTask, timerInterval);
    }

    @Override
    public void saveGameState(final Uri uri) {
        if (Thread.currentThread() != libQspHandler.getLooper().getThread()) {
            runOnQspThread(() -> saveGameState(uri));
            return;
        }

        byte[] gameData = nativeMethods.QSPSaveGameAsData(false);
        if (gameData == null) {
            return;
        }
        try (OutputStream out = context.getContentResolver().openOutputStream(uri, "w")) {
            out.write(gameData);
        } catch (IOException e) {
            logger.error("Failed to save the game state", e);
        }
    }

    // endregion LibQspProxy

    // region LibQspCallbacks

    public void RefreshInt() {
        boolean confChanged = loadUIConfiguration();

        boolean mainDescChanged = nativeMethods.QSPIsMainDescChanged();
        if (mainDescChanged) {
            viewState.setMainDesc(nativeMethods.QSPGetMainDesc());
        }
        boolean actionsChanged = nativeMethods.QSPIsActionsChanged();
        if (actionsChanged) {
            loadActions();
        }
        boolean objectsChanged = nativeMethods.QSPIsObjectsChanged();
        if (objectsChanged) {
            loadObjects();
        }
        boolean varsDescChanged = nativeMethods.QSPIsVarsDescChanged();
        if (varsDescChanged) {
            viewState.setVarsDesc(nativeMethods.QSPGetVarsDesc());
        }
        PlayerView view = playerView;
        if (view != null) {
            playerView.refreshGameView(
                    confChanged,
                    mainDescChanged,
                    actionsChanged,
                    objectsChanged,
                    varsDescChanged);
        }
    }

    public void ShowPicture(String path) {
        PlayerView view = playerView;
        if (view == null || path == null || path.isEmpty()) {
            return;
        }
        view.showPicture(path);
    }

    public void SetTimer(int msecs) {
        timerInterval = msecs;
    }

    public void ShowMessage(String message) {
        PlayerView view = playerView;
        if (view != null) {
            view.showMessage(message);
        }
    }

    public void PlayFile(String path, int volume) {
        if (path != null && !path.isEmpty()) {
            audioPlayer.playFile(normalizePath(path), volume);
        }
    }

    public boolean IsPlayingFile(final String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        return audioPlayer.isPlayingFile(normalizePath(path));
    }

    public void CloseFile(String path) {
        if (path == null || path.isEmpty()) {
            audioPlayer.closeAllFiles();
        } else {
            audioPlayer.closeFile(normalizePath(path));
        }
    }

    public void OpenGame(String filename) {
        File savesDir = getOrCreateDirectory(viewState.getGameDir(), "saves");
        File saveFile = findFileOrDirectory(savesDir, filename);
        if (saveFile == null) {
            logger.error("Save file not found: " + filename);
            return;
        }
        loadGameState(Uri.fromFile(saveFile));
    }

    public void SaveGame(String filename) {
        PlayerView view = playerView;
        if (view != null) {
            view.showSaveGamePopup(filename);
        }
    }

    public String InputBox(String prompt) {
        PlayerView view = playerView;
        return view != null ? view.showInputBox(prompt) : null;
    }

    public int GetMSCount() {
        long now = SystemClock.elapsedRealtime();
        if (lastMsCountCallTime == 0) {
            lastMsCountCallTime = gameStartTime;
        }
        int dt = (int) (now - lastMsCountCallTime);
        lastMsCountCallTime = now;

        return dt;
    }

    public void AddMenuItem(String name, String imgPath) {
        QspMenuItem item = new QspMenuItem();
        item.imgPath = normalizePath(imgPath);
        item.name = name;
        viewState.getMenuItems().add(item);
    }

    public void ShowMenu() {
        PlayerView view = playerView;
        if (view == null) {
            return;
        }
        int result = view.showMenu();
        if (result != -1) {
            nativeMethods.QSPSelectMenuItem(result);
        }
    }

    public void DeleteMenu() {
        viewState.getMenuItems().clear();
    }

    public void Wait(int msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) {
            logger.error("Wait failed", e);
        }
    }

    public void ShowWindow(int type, boolean isShow) {
        WindowType windowType = WindowType.values()[type];
        playerView.showWindow(windowType, isShow);
    }

    public byte[] GetFileContents(String path) {
        String normPath = normalizePath(path);
        return getFileContents(normPath);
    }

    public void ChangeQuestPath(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            logger.error("Game directory not found: " + path);
            return;
        }
        if (!viewState.getGameDir().equals(dir)) {
            viewState.setGameDir(dir);
            imageProvider.invalidateCache();
        }
    }

    // endregion LibQspCallbacks
}
