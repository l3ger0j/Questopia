package com.qsp.player.install;

import static com.qsp.player.shared.util.ArchiveUtil.unrar;
import static com.qsp.player.shared.util.ArchiveUtil.unzip;
import static com.qsp.player.shared.util.ViewUtil.showErrorDialog;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import com.qsp.player.R;

import java.io.File;

public class ArchiveGameInstaller extends GameInstaller {
    private final ArchiveType archiveType;

    public ArchiveGameInstaller(Context context, ArchiveType archiveType) {
        super(context);
        this.archiveType = archiveType;
    }

    @Override
    public boolean install(String gameName, DocumentFile srcFile, File destDir) {
        boolean extracted = false;
        if (archiveType == ArchiveType.ZIP) {
            extracted = unzip(context, srcFile, destDir);
        } else if (archiveType == ArchiveType.RAR) {
            extracted = unrar(context, srcFile, destDir);
        }
        if (!extracted) {
            String message = context.getString(R.string.installError).replace("-GAMENAME-", gameName);
            showErrorDialog(context, message);
            return false;
        }
        return postInstall(destDir);
    }
}
