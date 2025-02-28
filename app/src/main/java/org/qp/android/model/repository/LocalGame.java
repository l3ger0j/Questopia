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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.file.DocumentFileType;
import com.anggrayudi.storage.file.DocumentFileUtils;
import com.anggrayudi.storage.file.MimeType;

import org.qp.android.dto.stock.GameData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @WorkerThread
    public void searchAndWriteData(File rootDir, GameData data) {
        if (!isWritableDir(context, rootDir)) {
            return;
        }

        var subFiles = Collections.synchronizedList(new ArrayList<Uri>());
        synchronized (subFiles) {
            try (var walk = Files.walk(rootDir.toPath())) {
                walk.map(Path::toFile)
                        .filter(f -> f.isFile() && f.getPath().endsWith(".qsp") || f.getPath().endsWith(".gam"))
                        .map(Uri::fromFile)
                        .forEach(subFiles::add);
            } catch (IOException e) {
                return;
            }
        }

        data.gameDirUri = Uri.fromFile(rootDir);
        data.gameFilesUri = subFiles;

        createDataIntoFolder(data, rootDir);
    }

    @WorkerThread
    public void searchAndWriteFileInfo(DocumentFile rootDir) {
        if (!isWritableDir(context, rootDir)) {
            return;
        }

        var gameFiles = Collections.synchronizedList(new ArrayList<Uri>());
        var files = DocumentFileUtils.search(rootDir, true, DocumentFileType.FILE);

        files.parallelStream().forEach(d -> {
            var dirExtension = documentWrap(d).getExtension();
            var lcName = dirExtension.toLowerCase(Locale.ROOT);
            if (lcName.endsWith("qsp") || lcName.endsWith("gam")) {
                gameFiles.add(d.getUri());
            }
        });

        var infoFile = fromRelPath(context, GAME_INFO_FILENAME, rootDir);
        if (isWritableFile(context, infoFile)) {
            GameData data = null;
            var infoFileCont = readFileAsString(context, infoFile.getUri());
            if (isNotEmptyOrBlank(infoFileCont)) {
                try {
                    data = parseGameInfo(infoFileCont);
                } catch (IOException e) {
                    return;
                }
            }
            if (data != null) {
                if (!Objects.equals(data.gameDirUri.getPath(), rootDir.getUri().getPath())) {
                    data.gameDirUri = rootDir.getUri();
                }

                if (!Objects.deepEquals(data.gameFilesUri, gameFiles)) {
                    data.gameFilesUri = gameFiles;
                }

                createDataIntoFolder(data, rootDir);
            }
        }

        createNoMediaFile(rootDir);
        createNoSearchFile(rootDir);
    }

    public List<GameData> lightExtractDataFromDir(File generalGamesDir) throws IOException {
        if (!isWritableDir(context, generalGamesDir)) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = Collections.synchronizedList(new ArrayList<GameData>());
        var subRootDir = Collections.synchronizedList(new ArrayList<File>());
        synchronized (subRootDir) {
            try (var walk = Files.walk(generalGamesDir.toPath(), 1)) {
                walk.map(Path::toFile)
                        .filter(file -> file.isDirectory() && !Objects.deepEquals(file, generalGamesDir))
                        .forEach(subRootDir::add);
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        synchronized (itemsGamesDirs) {
            for (var dir : subRootDir) {
                if (!isWritableDir(context, dir)) {
                    continue;
                }
                var item = (GameData) null;
                var infoFile = fromRelPath(GAME_INFO_FILENAME, dir);
                if (isWritableFile(context, infoFile)) {
                    var infoFileCont = readFileAsString(infoFile);
                    if (isNotEmptyOrBlank(infoFileCont)) {
                        try {
                            item = parseGameInfo(infoFileCont);
                        } catch (IOException e) {
                            continue;
                        }
                    }
                    if (item != null) {
                        itemsGamesDirs.add(item);
                    }
                }
            }
        }

        return itemsGamesDirs;
    }

    public List<GameData> lightExtractDataFromList(List<DocumentFile> fileList) throws IOException {
        if (fileList.isEmpty()) {
            return Collections.emptyList();
        }

        var itemsGamesDirs = Collections.synchronizedList(new ArrayList<GameData>());
        var unpackFileList = Collections.synchronizedList(new ArrayList<>(fileList));

        synchronized (itemsGamesDirs) {
            for (var data : unpackFileList) {
                if (!isWritableDir(context, data)) {
                    continue;
                }
                var item = (GameData) null;
                var infoFile = fromRelPath(context, GAME_INFO_FILENAME, data);
                if (isWritableFile(context, infoFile)) {
                    var infoFileCont = readFileAsString(context, infoFile.getUri());
                    if (isNotEmptyOrBlank(infoFileCont)) {
                        try {
                            item = parseGameInfo(infoFileCont);
                        } catch (IOException e) {
                            continue;
                        }
                    }
                    if (item != null) {
                        itemsGamesDirs.add(item);
                    }
                }
            }
        }

        return itemsGamesDirs;
    }

    @Nullable
    private GameData parseGameInfo(String json) throws IOException {
        return jsonToObject(json, GameData.class);
    }

    private record GameFolder(Uri gameUriDir, List<Uri> gameUriFiles) {}
}