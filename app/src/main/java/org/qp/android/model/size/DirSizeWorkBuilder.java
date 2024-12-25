package org.qp.android.model.size;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public final class DirSizeWorkBuilder {

    public static LiveData<WorkInfo> startCalcSizeDir(Context context, Uri srcDir) {
        var inputData = new Data.Builder()
                .putString("srcDir", srcDir.toString())
                .build();
        var workRequest = new OneTimeWorkRequest.Builder(DirSizeWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(workRequest.getId());
    }
    //        WorkManager.getInstance(context)
//                .getWorkInfoByIdLiveData(workRequest.getId()).observeForever(workInfo -> {
//                    if (workInfo.getState().isFinished()) {
//                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//                            if (!workInfo.getOutputData().equals(Data.EMPTY)) {
//                                dirSize.postValue(workInfo.getOutputData().getLong("dirSize" , 0L));
//                            }
//                        }
//                    }
//                });
}
