package org.qp.android.ui.stock;

import android.net.Uri;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

public class GameDataObserver {

    public ObservableBoolean isModDirExist = new ObservableBoolean();
    public ObservableBoolean isGameInstalled = new ObservableBoolean();
    public ObservableField<String> authorObserver = new ObservableField<>();
    public ObservableField<String> portedByObserver = new ObservableField<>();
    public ObservableField<String> versionObserver = new ObservableField<>();
    public ObservableField<String> titleObserver = new ObservableField<>();
    public ObservableField<String> langObserver = new ObservableField<>();
    public ObservableField<String> playerObserver = new ObservableField<>();
    public ObservableField<String> iconPathObserver = new ObservableField<>();
    public ObservableField<Uri> iconUriObserver = new ObservableField<>();
    public ObservableField<String> fileUrlObserver = new ObservableField<>();
    public ObservableField<String> fileSizeObserver = new ObservableField<>();
    public ObservableField<String> fileExtObserver = new ObservableField<>();
    public ObservableField<String> descUrlObserver = new ObservableField<>();
    public ObservableField<String> pubDateObserver = new ObservableField<>();
    public ObservableField<String> modDateObserver = new ObservableField<>();

}
