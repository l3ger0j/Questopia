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

import android.content.Context;
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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LocalGame {

    private final String TAG = this.getClass().getSimpleName();

    private static final String GAME_INFO_FILENAME = ".gameInfo";
    private static final String NOMEDIA_FILENAME = ".nomedia";
    private static final String NOSEARCH_FILENAME = ".nosearch";

    private final Context context;

    @NonNull
    private List<File> getGameDirectories(Path generalGamesDir) throws IOException {
        var gamesDirsList = new ArrayList<File>();

        Files.walkFileTree(generalGamesDir, Collections.singleton(FileVisitOption.FOLLOW_LINKS), 2, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (!dir.equals(generalGamesDir)) {
                    gamesDirsList.add(dir.toFile());
                }
                return super.postVisitDirectory(dir , exc);
            }
        });

        return gamesDirsList;
    }

    public LocalGame(Context context) {
        this.context = context;
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
            Log.e(TAG , "IS NOT WRITABLE");
            return;
        }

        var tempInfoFile = documentWrap(infoFile);
        try (var out = tempInfoFile.openOutputStream(context, false)) {
            objectToJson(out , gameData);
        } catch (Exception ex) {
            Log.e(TAG , "ERROR: " , ex);
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
        if (generalGamesFolder == null) {
            return Collections.emptyList();
        }

        var gamesDirs = getGameDirectories(generalGamesFolder.toPath());
        if (gamesDirs.isEmpty()) {
            Log.e(TAG, "game dir is null");
            return Collections.emptyList();
        }

        var itemsGamesDirs = new ArrayList<GameData>();
        var formatGamesDirs = wrapFToGameFolder(gamesDirs);
        var random = new SecureRandom();

        for (var gameFolder : formatGamesDirs) {
            var gameDir = new File(gameFolder.gameUriDir.getPath());
            if (!isWritableDir(context, gameDir)) return Collections.emptyList();

            var item = (GameData) null;
            var infoFile = fromRelPath(GAME_INFO_FILENAME, gameDir);

            if (!isWritableFile(context, infoFile)) {
                var name = gameDir.getName();
                if (name.isEmpty()) return Collections.emptyList();

                item = new GameData();
                item.id = random.nextInt();
                item.title = name;
                item.gameDir = gameFolder.gameUriDir;
                item.gameFiles = gameFolder.gameUriFiles;

                createDataIntoFolder(item, gameDir);
                itemsGamesDirs.add(item);
            } else {
                var infoFileCont = readFileAsString(infoFile);

                if (infoFileCont != null) {
                    item = parseGameInfo(infoFileCont);
                }

                if (item == null) {
                    var name = gameDir.getName();
                    if (name.isEmpty()) return Collections.emptyList();

                    item = new GameData();
                    item.id = random.nextInt();
                    item.title = name;
                    item.gameDir = gameFolder.gameUriDir;
                    item.gameFiles = gameFolder.gameUriFiles;

                    createDataIntoFolder(item, gameDir);
                    itemsGamesDirs.add(item);
                } else {
                    item.gameDir = gameFolder.gameUriDir;
                    item.gameFiles = gameFolder.gameUriFiles;

                    itemsGamesDirs.add(item);
                }
            }
        }

        return itemsGamesDirs;
    }

    public List<GameData> extractDataFromList(List<DocumentFile> fileList) throws IOException {
        if (fileList.isEmpty()) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = new ArrayList<GameData>();
        var formatGamesDirs = wrapDToGameFolder(fileList);
        var random = new SecureRandom();

        for (var gameFolder : formatGamesDirs) {
            var gameDir = DocumentFileCompat.fromUri(context, gameFolder.gameUriDir);
            if (!isWritableDir(context, gameDir)) return Collections.emptyList();

            var item = (GameData) null;
            var infoFile = fromRelPath(context, GAME_INFO_FILENAME, gameDir);

            if (infoFile == null) {
                var name = gameDir.getName();
                if (name == null) return Collections.emptyList();

                item = new GameData();
                item.id = random.nextInt();
                item.title = name;
                item.gameDir = gameFolder.gameUriDir;
                item.gameFiles = gameFolder.gameUriFiles;

                createDataIntoFolder(item, gameDir);
                itemsGamesDirs.add(item);
            } else {
                var infoFileCont = readFileAsString(context , infoFile.getUri());

                if (infoFileCont != null) {
                    item = parseGameInfo(infoFileCont);
                }

                if (item == null) {
                    var name = gameDir.getName();
                    if (name == null) return Collections.emptyList();

                    item = new GameData();
                    item.id = random.nextInt();
                    item.title = name;
                    item.gameDir = gameFolder.gameUriDir;
                    item.gameFiles = gameFolder.gameUriFiles;

                    createDataIntoFolder(item, gameDir);
                    itemsGamesDirs.add(item);
                } else {
                    item.gameDir = gameFolder.gameUriDir;
                    item.gameFiles = gameFolder.gameUriFiles;
                    itemsGamesDirs.add(item);
                }
            }

            createNoMediaFile(gameDir);
            createNoSearchFile(gameDir);
        }

        return itemsGamesDirs;
    }

    @Nullable
    private GameData parseGameInfo(String json) throws IOException {
        return jsonToObject(json, GameData.class);
    }

    @NonNull
    private List<GameFolder> wrapDToGameFolder(List<DocumentFile> dirs) {
        var folders = new ArrayList<GameFolder>();

        for (var rootDir : dirs) {
            var gameFiles = new ArrayList<Uri>();
            var files = rootDir.listFiles();
            if (files == null) return Collections.emptyList();

            for (var file : files) {
                if (file.getName() == null) continue;
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file.getUri());
                }
            }

            folders.add(new GameFolder(rootDir.getUri(), gameFiles));
        }

        return folders;
    }

    @NonNull
    private List<GameFolder> wrapFToGameFolder(List<File> dirs) {
        var folders = new ArrayList<GameFolder>();

        for (var dir : dirs) {
            var gameFiles = new ArrayList<Uri>();
            var files = dir.listFiles();
            if (files == null) return Collections.emptyList();

            for (var file : files) {
                if (file.getName().isEmpty()) continue;
                var lcName = file.getName().toLowerCase(Locale.ROOT);
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(Uri.fromFile(file));
                }
            }

            folders.add(new GameFolder(Uri.fromFile(dir), gameFiles));
        }

        return folders;
    }

    private record GameFolder(Uri gameUriDir, List<Uri> gameUriFiles) {}
}