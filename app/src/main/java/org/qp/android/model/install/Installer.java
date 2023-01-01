package org.qp.android.model.install;

import static org.qp.android.utils.DirUtil.doesDirectoryContainGameFiles;
import static org.qp.android.utils.DirUtil.normalizeGameDirectory;
import static org.qp.android.utils.FileUtil.copyFile;
import static org.qp.android.utils.FileUtil.getOrCreateDirectory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;

public class Installer {
    private final MutableLiveData<Boolean> isDone = new MutableLiveData<>();
    private final Context context;

    public Installer(Context context) {
        this.context = context;
    }

    public MutableLiveData<Boolean> gameInstall (@NonNull DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            installDirectory(srcFile, destDir);
        } else {
            installArchive(srcFile , destDir);
        }
        return isDone;
    }

    private void installArchive (DocumentFile srcFile, File destDir) {
        var inputData = new Data.Builder()
                .putString("srcFile", srcFile.getUri().toString())
                .putString("destDir", destDir.getAbsolutePath())
                .build();

        var workRequest = new OneTimeWorkRequest.Builder(InstallerWork.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.getId()).observeForever(workInfo -> {
            if (workInfo.getState().isFinished()) {
                switch (workInfo.getState()) {
                    case SUCCEEDED:
                        isDone.postValue(true);
                        break;
                    case FAILED:
                        isDone.postValue(false);
                        if (!workInfo.getOutputData().equals(Data.EMPTY)) {
                            if (workInfo.getOutputData().getString("errorOne") != null) {
                                throw new InstallException("NIG");
                            } else if (workInfo.getOutputData().getString("errorTwo") != null) {
                                throw new InstallException("NFE");
                            }
                        }
                        break;
                }
            }
        });
    }

    private void installDirectory(DocumentFile srcFile , File destDir) {
        for (DocumentFile file : srcFile.listFiles()) {
            copyFileOrDirectory(file, destDir);
        }
        normalizeGameDirectory(destDir);
        if (!doesDirectoryContainGameFiles(destDir)) {
            throw new InstallException("NFE");
        }
        isDone.postValue(true);
    }

    private void copyFileOrDirectory(DocumentFile srcFile, File destDir) {
        if (srcFile.isDirectory()) {
            File subDestDir = getOrCreateDirectory(destDir, srcFile.getName());
            for (DocumentFile subSrcFile : srcFile.listFiles()) {
                copyFileOrDirectory(subSrcFile, subDestDir);
            }
        } else {
            copyFile(context, srcFile, destDir);
        }
    }
}
