package com.qsp.player.install;

import static com.qsp.player.shared.util.GameDirUtil.doesDirectoryContainGameFiles;
import static com.qsp.player.shared.util.GameDirUtil.normalizeGameDirectory;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.shared.util.ViewUtil;

import java.io.File;

public abstract class GameInstaller {
    protected final Context context;

    public GameInstaller(Context context) {
        this.context = context;
    }

    /**
     * @throws InstallException непредвиденная ошибка
     */
    public abstract boolean install(String gameName, DocumentFile srcFile, File destDir);

    protected boolean postInstall(File gameDir) {
        normalizeGameDirectory(gameDir);

        boolean containsGameFiles = doesDirectoryContainGameFiles(gameDir);
        if (!containsGameFiles) {
            ViewUtil.showErrorDialog(context, context.getString(R.string.noGameFilesError));
            return false;
        }

        return true;
    }
}
