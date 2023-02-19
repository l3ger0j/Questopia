package org.qp.android.plugin;

import org.qp.android.GameDataParcel;

interface AsyncCallback {
    void onSuccess(inout List<GameDataParcel> gameDataParcel);
}