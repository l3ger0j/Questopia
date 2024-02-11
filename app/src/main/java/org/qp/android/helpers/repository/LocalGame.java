package org.qp.android.helpers.repository;

import static org.qp.android.helpers.utils.FileUtil.createFindDFile;
import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.FileUtil.readFileAsString;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.MimeType;

import org.qp.android.dto.stock.GameData;
import org.qp.android.ui.settings.SettingsController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocalGame {

    private final String TAG = this.getClass().getSimpleName();

    private static final String GAME_INFO_FILENAME = ".gameInfo";
    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";

    @NonNull
    private List<GameFolder> getGamesFolders(@NonNull List<DocumentFile> dirs) {
        var folders = new ArrayList<GameFolder>();

        for (var dir : dirs) {
            var gameFiles = new ArrayList<DocumentFile>();
            var files = dir.listFiles();

            for (var file : files) {
                if (file.getName() == null) continue;
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file);
                }
            }

            folders.add(new GameFolder(dir , gameFiles));
        }

        return folders;
    }

    @Nullable
    private DocumentFile getGameInfoFile(@NonNull DocumentFile gameFolder) {
        var findGameInfoFile = gameFolder.findFile(GAME_INFO_FILENAME);
        if (findGameInfoFile == null) {
            Log.w(TAG , "GameData info file not found in " + gameFolder.getName());
            return null;
        }
        return findGameInfoFile;
    }

    private void createNoMediaFile(@NonNull DocumentFile gameDir) {
        var findNoMediaFile = gameDir.findFile(NOMEDIA_FILENAME);
        if (findNoMediaFile == null || !findNoMediaFile.exists()) {
            forceCreateFile(gameDir , MimeType.TEXT , NOMEDIA_FILENAME);
        }
    }

    private void createNoSearchFile(@NonNull DocumentFile gameDir) {
        var findNoSearchFile = gameDir.findFile(NOSEARCH_FILENAME);
        if (findNoSearchFile == null || !findNoSearchFile.exists()) {
            forceCreateFile(gameDir , MimeType.TEXT , NOSEARCH_FILENAME);
        }
    }

    public void createDataIntoFolder(Context context ,
                                     GameData gameData ,
                                     DocumentFile gameDir) {
        var infoFile = getGameInfoFile(gameDir);
        if (infoFile == null) {
            infoFile = createFindDFile(gameDir , MimeType.TEXT , GAME_INFO_FILENAME);
        }
        if (!isWritableFile(infoFile)) {
            Log.e(TAG , "ERROR");
            return;
        }
        var tempInfoFile = documentWrap(infoFile);

        try (var out = tempInfoFile.openOutputStream(context , false)) {
            objectToJson(out , gameData);
        } catch (Exception ex) {
            Log.e(TAG , "ERROR: " , ex);
        }
    }

    public List<GameData> extractGameDataFromList(Context context , List<DocumentFile> fileList) {
        if (fileList.isEmpty()) {
            return Collections.emptyList();
        }

        var controller = SettingsController.newInstance(context);
        var itemsGamesDirs = new ArrayList<GameData>();
        var formatGamesDirs = getGamesFolders(fileList);

        for (var gameFolder : formatGamesDirs) {
            var item = (GameData) null;
            var infoFile = getGameInfoFile(gameFolder.dir);

            if (infoFile == null) {
                var name = gameFolder.dir.getName();
                if (name == null) return Collections.emptyList();
                item = new GameData();
                item.id = name;
                item.title = name;
                item.gameDir = gameFolder.dir;
                item.gameFiles = gameFolder.gameFiles;
                createDataIntoFolder(context , item , gameFolder.dir);

                itemsGamesDirs.add(item);
            } else {
                var infoFileCont = readFileAsString(context , infoFile.getUri());

                if (infoFileCont != null) {
                    item = parseGameInfo(infoFileCont);
                }

                if (item == null) {
                    var name = gameFolder.dir.getName();
                    if (name == null) return Collections.emptyList();
                    item = new GameData();
                    item.id = name;
                    item.title = name;
                    item.gameDir = gameFolder.dir;
                    item.gameFiles = gameFolder.gameFiles;
                    createDataIntoFolder(context , item , gameFolder.dir);
                    itemsGamesDirs.add(item);
                } else {
                    item.gameDir = gameFolder.dir;
                    item.gameFiles = gameFolder.gameFiles;
                    itemsGamesDirs.add(item);
                }
            }

            if (controller.isCreateNoMedia) createNoMediaFile(gameFolder.dir);
            if (controller.isCreateNoSearch) createNoSearchFile(gameFolder.dir);
        }

        return itemsGamesDirs;
    }

    @Nullable
    private GameData parseGameInfo(String json) {
        try {
            return jsonToObject(json , GameData.class);
        } catch (Exception ex) {
            Log.e(TAG , "Failed to parse game info file" , ex);
            return null;
        }
    }

    private record GameFolder(DocumentFile dir , List<DocumentFile> gameFiles) {}
}