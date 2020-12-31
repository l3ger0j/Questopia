package com.qsp.player.service;

import java.io.File;

import static com.qsp.player.util.FileUtil.findFileRecursively;

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
     * Приводит к нормальной форме путь до игрового ресурса (мелодии, изображения).
     *
     * @implNote Удаляет "./" из начала пути, заменяет все вхождения "\" на "/".
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
