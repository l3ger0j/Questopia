package org.qp.android.viewModel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.qp.android.dto.FileInfo;

import java.util.ArrayList;

public class FilePickerViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<FileInfo>> fileInfoList;

    FilePickerViewModel() {
        fileInfoList = new MutableLiveData<>();
    }

    public MutableLiveData<ArrayList<FileInfo>> getFileInfoList() {
        return fileInfoList;
    }

    public void setFileInfoList(ArrayList<FileInfo> fileInfoArrayList) {
        fileInfoList.postValue(fileInfoArrayList);
    }
}
