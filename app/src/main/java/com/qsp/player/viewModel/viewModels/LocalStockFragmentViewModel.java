package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.activities.GameStockActivity;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;

import java.util.ArrayList;

public class LocalStockFragmentViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<GameData>> gameDataList;

    public ObservableField<GameStockActivity> activityObservableField =
            new ObservableField<>();

    public MutableLiveData<ArrayList<GameData>> getGameData() {
        return gameDataList;
    }

    public void setGameDataArrayList(ArrayList<GameData> gameDataArrayList) {
        gameDataList.postValue(gameDataArrayList);
    }

    public LocalStockFragmentViewModel () {
        gameDataList = new MutableLiveData<>();
    }
}
