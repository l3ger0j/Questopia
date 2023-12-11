package org.qp.android.model.service;

import static org.qp.android.helpers.utils.FileUtil.documentWrap;
import static org.qp.android.helpers.utils.FileUtil.findFileOrDirectory;
import static org.qp.android.helpers.utils.PathUtil.normalizeContentPath;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.anggrayudi.storage.FileWrapper;
import com.anggrayudi.storage.file.DocumentFileCompat;

public class GameContentResolver {

    @Nullable
    public FileWrapper.Document getFile(@NonNull Context context ,
                                        @NonNull DocumentFile gameDir ,
                                        String relPath) {
        var tempFile = findFileOrDirectory(gameDir , normalizeContentPath(relPath));
        if (tempFile == null) {
            var fullPathToGameDir = documentWrap(gameDir).getAbsolutePath(context);
            tempFile = DocumentFileCompat.fromFullPath(
                    context , fullPathToGameDir  + "/" + relPath);
        }

        try {
            return documentWrap(tempFile);
        } catch (NullPointerException e) {
            Log.d(this.getClass().getSimpleName() , "Error: " , e);
            return null;
        }
    }

}
