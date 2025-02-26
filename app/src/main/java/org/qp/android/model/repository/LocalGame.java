package org.qp.android.model.repository;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findOrCreateFile;
import static org.qp.android.helpers.utils.FileUtil.forceCreateFile;
import static org.qp.android.helpers.utils.FileUtil.fromRelPath;
import static org.qp.android.helpers.utils.FileUtil.isWritableDir;
import static org.qp.android.helpers.utils.FileUtil.isWritableFile;
import static org.qp.android.helpers.utils.FileUtil.readFileAsString;
import static org.qp.android.helpers.utils.JsonUtil.jsonToObject;
import static org.qp.android.helpers.utils.JsonUtil.objectToJson;
import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileCompat;
import com.anggrayudi.storage.file.MimeType;

import org.qp.android.dto.stock.GameData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class LocalGame {

    private static final String GAME_INFO_FILENAME = ".gameInfo";
    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";
    private final String TAG = this.getClass().getSimpleName();
    private final Context context;

    public LocalGame(Context context) {
        this.context = context;
    }

    private void dropPersistable(Uri folderUri) {
        try {
            var contentResolver = context.getContentResolver();
            contentResolver.releasePersistableUriPermission(
                    folderUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }
    }

    private void createNoMediaFile(@NonNull DocumentFile gameDir) {
        forceCreateFile(context, gameDir, MimeType.TEXT, NOMEDIA_FILENAME);
    }

    private void createNoSearchFile(@NonNull DocumentFile gameDir) {
        forceCreateFile(context, gameDir, MimeType.TEXT, NOSEARCH_FILENAME);
    }

    public void createDataIntoFolder(GameData gameData, DocumentFile gameDir) {
        var infoFile = findOrCreateFile(context, gameDir, GAME_INFO_FILENAME, MimeType.TEXT);

        if (!isWritableFile(context, infoFile)) {
            Log.e(TAG, "IS NOT WRITABLE");
            return;
        }

        var tempInfoFile = documentWrap(infoFile);
        try (var out = tempInfoFile.openOutputStream(context, false)) {
            objectToJson(out, gameData);
        } catch (Exception ex) {
            Log.e(TAG, "ERROR: ", ex);
        }
    }

    public void createDataIntoFolder(GameData data, File gameDir) {
        var infoFile = findOrCreateFile(context, gameDir, GAME_INFO_FILENAME, MimeType.TEXT);

        if (!isWritableFile(context, infoFile)) {
            Log.e(TAG, "IS NOT WRITABLE");
            return;
        }

        try (var out = new FileOutputStream(infoFile)) {
            objectToJson(out, data);
        } catch (Exception ex) {
            Log.e(TAG, "ERROR: ", ex);
        }
    }

    public List<GameData> extractDataFromFolder(File generalGamesFolder) throws IOException {
        if (!isWritableDir(context, generalGamesFolder)) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = Collections.synchronizedList(new ArrayList<GameData>());
        var formatGamesDirs = wrapFToGameFolder(generalGamesFolder);
        var random = new SecureRandom();

        synchronized (itemsGamesDirs) {
            for (var gameFolder : formatGamesDirs) {
                var gameDir = new File(gameFolder.gameUriDir.getPath());
                if (!isWritableDir(context, gameDir)) {
                    dropPersistable(gameFolder.gameUriDir);
                    formatGamesDirs.remove(gameFolder);
                    continue;
                }

                var item = (GameData) null;
                var infoFile = fromRelPath(GAME_INFO_FILENAME, gameDir);

                if (!isWritableFile(context, infoFile)) {
                    var name = gameDir.getName();
                    if (!isNotEmptyOrBlank(name)) {
                        name = "game#" + random.nextInt();
                    }

                    item = new GameData();
                    item.id = random.nextInt();
                    item.title = name;
                    item.gameDirUri = gameFolder.gameUriDir;
                    item.gameFilesUri = gameFolder.gameUriFiles;

                    createDataIntoFolder(item, gameDir);
                    itemsGamesDirs.add(item);
                } else {
                    var infoFileCont = readFileAsString(infoFile);

                    if (isNotEmptyOrBlank(infoFileCont)) {
                        try {
                            item = parseGameInfo(infoFileCont);
                        } catch (IOException e) {
                            continue;
                        }
                    }

                    if (item == null) {
                        var name = gameDir.getName();
                        if (!isNotEmptyOrBlank(name)) {
                            name = "game#" + random.nextInt();
                        }

                        item = new GameData();
                        item.id = random.nextInt();
                        item.title = name;
                        item.gameDirUri = gameFolder.gameUriDir;
                        item.gameFilesUri = gameFolder.gameUriFiles;

                        createDataIntoFolder(item, gameDir);
                        itemsGamesDirs.add(item);
                    } else {
                        if (!Objects.equals(item.gameDirUri.getPath(), gameFolder.gameUriDir.getPath())) {
                            item.gameDirUri = gameFolder.gameUriDir;
                        }

                        if (!Objects.deepEquals(item.gameFilesUri, gameFolder.gameUriFiles)) {
                            item.gameFilesUri = gameFolder.gameUriFiles;
                        }

                        itemsGamesDirs.add(item);
                    }
                }
            }
        }

        return itemsGamesDirs;
    }

    public List<GameData> extractDataFromList(List<DocumentFile> fileList) throws IOException {
        if (fileList.isEmpty()) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = Collections.synchronizedList(new ArrayList<GameData>());
        var formatGamesDirs = wrapDToGameFolder(fileList);
        var random = new SecureRandom();

        synchronized (itemsGamesDirs) {
            for (var gameFolder : formatGamesDirs) {
                var gameDir = DocumentFileCompat.fromUri(context, gameFolder.gameUriDir);
                if (!isWritableDir(context, gameDir)) {
                    continue;
                }

                var item = (GameData) null;
                var infoFile = fromRelPath(context, GAME_INFO_FILENAME, gameDir);

                if (!isWritableFile(context, infoFile)) {
                    var name = gameDir.getName();
                    if (!isNotEmptyOrBlank(name)) {
                        name = "game#" + random.nextInt();
                    }

                    item = new GameData();
                    item.id = random.nextInt();
                    item.title = name;
                    item.gameDirUri = gameFolder.gameUriDir;
                    item.gameFilesUri = gameFolder.gameUriFiles;

                    createDataIntoFolder(item, gameDir);
                    itemsGamesDirs.add(item);
                } else {
                    var infoFileCont = readFileAsString(context, infoFile.getUri());

                    if (isNotEmptyOrBlank(infoFileCont)) {
                        try {
                            item = parseGameInfo(infoFileCont);
                        } catch (IOException e) {
                            continue;
                        }
                    }

                    if (item == null) {
                        var name = gameDir.getName();
                        if (!isNotEmptyOrBlank(name)) {
                            name = "game#" + random.nextInt();
                        }

                        item = new GameData();
                        item.id = random.nextInt();
                        item.title = name;
                        item.gameDirUri = gameFolder.gameUriDir;
                        item.gameFilesUri = gameFolder.gameUriFiles;

                        createDataIntoFolder(item, gameDir);
                        itemsGamesDirs.add(item);
                    } else {
                        if (!Objects.equals(item.gameDirUri.getPath(), gameFolder.gameUriDir.getPath())) {
                            item.gameDirUri = gameFolder.gameUriDir;
                        }

                        if (!Objects.deepEquals(item.gameFilesUri, gameFolder.gameUriFiles)) {
                            item.gameFilesUri = gameFolder.gameUriFiles;
                        }

                        itemsGamesDirs.add(item);
                    }
                }

                createNoMediaFile(gameDir);
                createNoSearchFile(gameDir);
            }
        }

        return itemsGamesDirs;
    }

    @Nullable
    private GameData parseGameInfo(String json) throws IOException {
        return jsonToObject(json, GameData.class);
    }

    @NonNull
    private List<GameFolder> wrapDToGameFolder(List<DocumentFile> dirs) {
        var folders = Collections.synchronizedList(new ArrayList<GameFolder>());
        var dirsList = Collections.synchronizedList(dirs);

        synchronized (dirsList) {
            for (var rootDir : dirs) {
                var gameFiles = new ArrayList<Uri>();
                var files = rootDir.listFiles();

                for (var file : files) {
                    var dirExtension = documentWrap(file).getExtension();
                    var lcName = dirExtension.toLowerCase(Locale.ROOT);
                    if (lcName.endsWith("qsp") || lcName.endsWith("gam")) {
                        gameFiles.add(file.getUri());
                    }
                }

                folders.add(new GameFolder(rootDir.getUri(), gameFiles));
            }
        }

        return folders;
    }

    @NonNull
    private List<GameFolder> wrapFToGameFolder(File rootDir) {
        var folders = Collections.synchronizedList(new ArrayList<GameFolder>());

        var subRootDir = Collections.synchronizedList(new ArrayList<File>());
        synchronized (subRootDir) {
            try (var walk = Files.walk(rootDir.toPath(), 1)) {
                walk.map(Path::toFile)
                        .filter(file -> file.isDirectory() && !Objects.deepEquals(file, rootDir))
                        .forEach(subRootDir::add);
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        var subFiles = Collections.synchronizedList(new ArrayList<Uri>());
        synchronized (subFiles) {
            for (var element : subRootDir) {
                try (var walk = Files.walk(element.toPath())) {
                    walk.map(Path::toFile)
                            .filter(f -> f.isFile() && f.getPath().endsWith(".qsp") || f.getPath().endsWith(".gam"))
                            .map(Uri::fromFile)
                            .forEach(subFiles::add);
                } catch (IOException e) {
                    return Collections.emptyList();
                }
                folders.add(new GameFolder(Uri.fromFile(element), subFiles));
            }
        }

        return folders;
    }

    private record GameFolder(Uri gameUriDir, List<Uri> gameUriFiles) {}
}