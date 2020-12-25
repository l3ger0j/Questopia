package com.qsp.player.stock.install;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.util.ViewUtil;

import java.io.File;

import static com.qsp.player.util.GameDirUtil.doesDirectoryContainGameFiles;
import static com.qsp.player.util.GameDirUtil.normalizeGameDirectory;

public abstract class GameInstaller {

    protected final Context context;

    protected DocumentFile gameFileOrDir;
    protected String gameName;

    public GameInstaller(Context context) {
        this.context = context;
    }

    /**
     * @throws InstallException непредвиденная ошибка
     */
    public abstract void load(Uri uri);

    /**
     * @throws InstallException непредвиденная ошибка
     */
    public abstract boolean install(File gameDir);

    public String getGameName() {
        return gameName;
    }

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
