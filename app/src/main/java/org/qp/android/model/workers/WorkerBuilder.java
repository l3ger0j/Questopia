package org.qp.android.model.workers;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class WorkerBuilder {
    private final MutableLiveData<Boolean> isDone = new MutableLiveData<>();
    private final MutableLiveData<String> errorCode = new MutableLiveData<>();

    private final MutableLiveData<Long> dirSize = new MutableLiveData<>();
    private final Context context;

    public LiveData<String> getErrorCode() {
        return errorCode;
    }

    public WorkerBuilder(Context context) {
        this.context = context;
    }

    public LiveData<Boolean> copyDirToAnotherDir(DocumentFile srcDir, DocumentFile destDir) {
        var inputData = new Data.Builder()
                .putString("srcDir", srcDir.getUri().toString())
                .putString("destDir", destDir.getUri().toString())
                .build();

        var workRequest = new OneTimeWorkRequest.Builder(CopyFolderWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context)
                .beginUniqueWork("copyDirToAnotherDir" ,
                        ExistingWorkPolicy.REPLACE , workRequest)
                .enqueue();

        WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.getId()).observeForever(workInfo -> {
                    if (workInfo.getState().isFinished()) {
                        switch (workInfo.getState()) {
                            case SUCCEEDED -> isDone.postValue(true);
                            case FAILED -> {
                                isDone.postValue(false);
                                if (!workInfo.getOutputData().equals(Data.EMPTY)) {
                                    if (workInfo.getOutputData().getString("errorTwo") != null) {
                                        errorCode.postValue("NFE");
                                    }
                                }
                            }
                        }
                    }
                });
        return isDone;
    }

    public LiveData<Long> calculateDirSize(DocumentFile srcDir) {
        var inputData = new Data.Builder()
                .putString("srcDir", srcDir.getUri().toString())
                .build();

        var workRequest = new OneTimeWorkRequest.Builder(SizeFolderWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workRequest.getId()).observeForever(workInfo -> {
                    if (workInfo.getState().isFinished()) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            if (!workInfo.getOutputData().equals(Data.EMPTY)) {
                                dirSize.postValue(workInfo.getOutputData().getLong("dirSize" , 0L));
                            }
                        }
                    }
                });
        return dirSize;
    }
}
