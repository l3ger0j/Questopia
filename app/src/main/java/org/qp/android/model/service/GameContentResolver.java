package org.qp.android.model.service;

import static org.qp.android.utils.FileUtil.findFileRecursively;

import java.io.File;

public class GameContentResolver {
    private File gameDir;

    public File getFile(String relPath) {
        if (gameDir == null) {
            throw new IllegalStateException("gameDir must not be null");
        }
        return findFileRecursively(gameDir, normalizeContentPath(relPath));
    }

    public String getAbsolutePath(String relPath) {
        File file = getFile(relPath);
        return file != null ? file.getAbsolutePath() : null;
    }

    /**
     * Leads to the normal form of the path to the game resource (melodies, images).
     *
     * @implNote Removes "./" from the beginning of the path, replaces all occurrences of "\" with "/".
     */
    public static String normalizeContentPath(String path) {
        if (path == null) return null;

        String result = path;
        if (result.startsWith("./")) {
            result = result.substring(2);
        }

        return result.replace("\\", "/");
    }

    public void setGameDir(File gameDir) {
        this.gameDir = gameDir;
    }
}
