package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.getOrCreateDirectory;
import static com.qsp.player.utils.FileUtil.isWritableDirectory;
import static com.qsp.player.utils.FileUtil.isWritableFile;
import static com.qsp.player.utils.PathUtil.normalizeFolderName;
import static com.qsp.player.utils.XmlUtil.objectToXml;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableField;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.AndroidViewModel;

import com.qsp.player.R;
import com.qsp.player.dto.stock.GameData;
import com.qsp.player.model.install.ArchiveGameInstaller;
import com.qsp.player.model.install.ArchiveType;
import com.qsp.player.model.install.FolderGameInstaller;
import com.qsp.player.model.install.GameInstaller;
import com.qsp.player.model.install.InstallException;
import com.qsp.player.model.install.InstallType;
import com.qsp.player.utils.ViewUtil;
import com.qsp.player.view.activities.GameStockActivity;
import com.qsp.player.viewModel.repository.LocalGameRepository;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GameStockActivityVM extends AndroidViewModel {
    private final LocalGameRepository localGameRepository = new LocalGameRepository();
    private final HashMap<InstallType, GameInstaller> installers = new HashMap<>();
    private final HashMap<String, GameData> gamesMap = new HashMap<>();

    private File gamesDir;
    private Logger logger;

    public ObservableField<GameStockActivity> activityObservableField = new
            ObservableField<>();

    // region Getter/Setter
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setGamesDir(File gamesDir) {
        this.gamesDir = gamesDir;
        localGameRepository.setGamesDirectory(gamesDir);
    }

    public HashMap<String, GameData> getGamesMap() {
        return gamesMap;
    }
    // endregion Getter/Setter

    public GameStockActivityVM(@NonNull Application application) {
        super(application);
        installers.put(InstallType.ZIP_ARCHIVE, new ArchiveGameInstaller(getApplication(), ArchiveType.ZIP));
        installers.put(InstallType.RAR_ARCHIVE, new ArchiveGameInstaller(getApplication(), ArchiveType.RAR));
        installers.put(InstallType.AQSP_ARCHIVE, new ArchiveGameInstaller(getApplication(), ArchiveType.ZIP));
        installers.put(InstallType.FOLDER, new FolderGameInstaller(getApplication()));
    }

    // region Game install
    @NonNull
    public File getOrCreateGameDirectory(String gameName) {
        String folderName = normalizeFolderName(gameName);
        return getOrCreateDirectory(gamesDir, folderName);
    }

    public void installGame(DocumentFile gameFile, InstallType type, GameData gameData) {
        if (!isWritableDirectory(gamesDir)) {
            logger.error("Games directory is not writable");
            return;
        }
        GameInstaller installer = installers.get(type);
        if (installer == null) {
            logger.error(String.format("Installer not found by install type '%s'", type));
            return;
        }
        try {
            doInstallGame(installer, gameFile, gameData);
        } catch (InstallException ex) {
            logger.error(ex.getMessage());
        }
    }

    public void doInstallGame(GameInstaller installer, DocumentFile gameFile, GameData gameData) {
        File gameDir = getOrCreateGameDirectory(gameData.title);
        if (!isWritableDirectory(gameDir)) {
            logger.error("GameData directory is not writable");
            return;
        }

        Callable<Boolean> task = () -> installer.install(gameData.title, gameFile, gameDir);
        FutureTask<Boolean> futureTask = new FutureTask<>(task);
        ExecutorService service = Executors.newCachedThreadPool();
        service.submit(futureTask);
        boolean installed = false;

        try {
            installed = futureTask.get();
        } catch (ExecutionException | InterruptedException | OutOfMemoryError e) {
            logger.error(e.toString());
        }

        if (installed) {
            writeGameInfo(gameData , gameDir, logger);
            refreshGames();
        }
    }

    public void writeGameInfo(GameData gameData , File gameDir, Logger logger) {
        File infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            logger.error("Game data info file is not writable");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(infoFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            logger.error("Failed to write to a gameData info file", ex);
        }
    }
    // endregion Game install

    // region Refresh
    public void refreshGamesDirectory() {
        File extFilesDir = getApplication().getExternalFilesDir(null);
        if (extFilesDir == null) {
            logger.error("External files directory not found");
            return;
        }
        File dir = getOrCreateDirectory(extFilesDir, "games");
        if (!isWritableDirectory(dir)) {
            logger.error("Games directory is not writable");
            String message = getApplication().getString(R.string.gamesDirError);
            ViewUtil.showErrorDialog(getApplication(), message);
            return;
        }
        setGamesDir(dir);
        refreshGames();
    }

    public void refreshGames() {
        gamesMap.clear();
        for (GameData localGameData : localGameRepository.getGames()) {
            GameData remoteGameData = gamesMap.get(localGameData.id);
            if (remoteGameData != null) {
                GameData aggregateGameData = new GameData(remoteGameData);
                aggregateGameData.gameDir = localGameData.gameDir;
                aggregateGameData.gameFiles = localGameData.gameFiles;
                gamesMap.put(localGameData.id, aggregateGameData);
            } else {
                gamesMap.put(localGameData.id, localGameData);
            }
        }
        Objects.requireNonNull(activityObservableField.get()).setRecyclerAdapter();
    }
    // endregion Refresh
}

