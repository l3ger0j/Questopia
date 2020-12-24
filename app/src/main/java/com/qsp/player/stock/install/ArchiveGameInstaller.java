package com.qsp.player.stock.install;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;
import com.qsp.player.util.FileUtil;
import com.qsp.player.util.ViewUtil;
import com.qsp.player.util.ZipUtil;

public class ArchiveGameInstaller extends GameInstaller {

    public ArchiveGameInstaller(Context context) {
        super(context);
    }

    @Override
    public void load(Uri uri) {
        gameFileOrDir = DocumentFile.fromSingleUri(context, uri);
        if (gameFileOrDir == null || !gameFileOrDir.exists()) {
            throw new InstallException("ZIP file not found: " + uri);
        }
        String filename = gameFileOrDir.getName();
        if (filename == null) {
            throw new InstallException("ZIP filename is null");
        }
        gameName = FileUtil.removeFileExtension(filename);
    }

    @Override
    public boolean install(DocumentFile gameDir) {
        boolean extracted = ZipUtil.unzip(context, gameFileOrDir, gameDir);
        if (!extracted) {
            String message = context.getString(R.string.extractError).replace("-GAMENAME-", gameName);
            ViewUtil.showErrorDialog(context, message);
            return false;
        }
        return postInstall(gameDir);
    }
}
