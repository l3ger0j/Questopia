package com.qsp.player.stock.install;

import static com.qsp.player.util.ArchiveUtil.unrar;
import static com.qsp.player.util.ArchiveUtil.unzip;
import static com.qsp.player.util.PathUtil.removeExtension;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.util.ViewUtil;

import java.io.File;

public class ArchiveGameInstaller extends GameInstaller {
    private final ArchiveType archiveType;

    public ArchiveGameInstaller(Context context, ArchiveType archiveType) {
        super(context);
        this.archiveType = archiveType;
    }

    @Override
    public void load(Uri uri) {
        gameFileOrDir = DocumentFile.fromSingleUri(context, uri);
        if (gameFileOrDir == null || !gameFileOrDir.exists()) {
            throw new InstallException("Archive file not found: " + uri);
        }
        String filename = gameFileOrDir.getName();
        if (filename == null) {
            throw new InstallException("Archive filename is null");
        }
        gameName = removeExtension(filename);
    }

    @Override
    public boolean install(File gameDir) {
        boolean extracted = false;
        if (archiveType == ArchiveType.ZIP) {
            extracted = unzip(context, gameFileOrDir, gameDir);
        } else if (archiveType == ArchiveType.RAR) {
            extracted = unrar(context, gameFileOrDir, gameDir);
        }
        if (extracted) {
            return postInstall(gameDir);
        } else {
            String message = context.getString(R.string.extractError).replace("-GAMENAME-", gameName);
            ViewUtil.showErrorDialog(context, message);
            return false;
        }
    }
}
