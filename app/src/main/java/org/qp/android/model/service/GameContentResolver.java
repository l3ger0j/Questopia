package org.qp.android.model.service;

import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.FileWrapper;

public class GameContentResolver {
    private DocumentFile gameDir;
    private Context context;

    public void setGameDir(DocumentFile gameDir , Context context) {
        this.gameDir = gameDir;
        this.context = context;
    }

    public FileWrapper.Document getFile(String relPath) {
        if (gameDir == null) {
            throw new IllegalStateException("gameDir must not be null");
        }
        return new FileWrapper.Document(findFileOrDirectory(gameDir , normalizeContentPath(relPath)));
    }

    @Nullable
    public String getAbsolutePath(String relPath) {
        var file = getFile(relPath);
        return file != null ? file.getAbsolutePath(context) : null;
    }

    /**
     * Leads to the normal form of the path to the game resource (melodies, images).
     *
     * @implNote Removes "./" from the beginning of the path, replaces all occurrences of "\" with "/".
     */
    public static String normalizeContentPath(String path) {
        if (path == null) return null;
        var result = path;
        if (result.startsWith("./")) {
            result = result.substring(2);
        }
        return result.replace("\\", "/");
    }
}
