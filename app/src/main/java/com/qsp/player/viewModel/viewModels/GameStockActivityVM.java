package com.qsp.player.viewModel.viewModels;

import static com.qsp.player.utils.FileUtil.GAME_INFO_FILENAME;
import static com.qsp.player.utils.FileUtil.createFile;
import static com.qsp.player.utils.FileUtil.findFileOrDirectory;
import static com.qsp.player.utils.FileUtil.isWritableFile;
import static com.qsp.player.utils.XmlUtil.objectToXml;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class GameStockActivityVM extends ViewModel {

    public GameStockActivityVM() {
    }

    @NonNull
    public static String normalizeGameFolderName(@NonNull String name) {
        String result = name.endsWith("...") ? name.substring(0, name.length() - 3) : name;

        return result.replaceAll("[:\"?*|<> ]", "_")
                .replace("__", "_");
    }

    public void writeGameInfo(GameData gameData , File gameDir, Logger logger) {
        File infoFile = findFileOrDirectory(gameDir, GAME_INFO_FILENAME);
        if (infoFile == null) {
            infoFile = createFile(gameDir, GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            logger.error("GameData info file is not writable");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(infoFile);
             OutputStreamWriter writer = new OutputStreamWriter(out)) {
            writer.write(objectToXml(gameData));
        } catch (Exception ex) {
            logger.error("Failed to write to a gameData info file", ex);
        }
    }

}