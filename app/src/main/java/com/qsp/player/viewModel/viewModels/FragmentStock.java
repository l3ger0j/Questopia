package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.activities.Stock;

import java.util.ArrayList;

public class FragmentStock extends ViewModel {
    private final MutableLiveData<ArrayList<GameData>> gameDataList;

    public ObservableField<Stock> activityObservableField =
            new ObservableField<>();

    public FragmentStock() {
        gameDataList = new MutableLiveData<>();
    }

    public MutableLiveData<ArrayList<GameData>> getGameData() {
        return gameDataList;
    }

    public void setGameDataArrayList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
    }
}
