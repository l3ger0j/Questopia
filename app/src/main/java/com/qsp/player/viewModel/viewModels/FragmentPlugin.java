package com.qsp.player.viewModel.viewModels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.PluginList;

import java.util.ArrayList;

public class FragmentPlugin extends ViewModel {
    private final MutableLiveData<ArrayList<PluginList>> pluginList;

    public FragmentPlugin() {
        pluginList = new MutableLiveData<>();
    }

    public MutableLiveData<ArrayList<PluginList>> getGameData() {
        return pluginList;
    }

    public void setGameDataArrayList(ArrayList<PluginList> gameDataArrayList) {
        pluginList.postValue(gameDataArrayList);
    }
}
