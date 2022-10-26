package org.qp.android.viewModel.viewModels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.qp.android.dto.plugin.PluginInfo;

import java.util.ArrayList;

public class FragmentPlugin extends ViewModel {
    private final MutableLiveData<ArrayList<PluginInfo>> pluginList;

    public FragmentPlugin() {
        pluginList = new MutableLiveData<>();
    }

    public MutableLiveData<ArrayList<PluginInfo>> getGameData() {
        return pluginList;
    }

    public void setGameDataArrayList(ArrayList<PluginInfo> gameDataArrayList) {
        pluginList.postValue(gameDataArrayList);
    }
}
