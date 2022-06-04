package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.activities.GameStockActivity;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;

import java.util.ArrayList;

public class RemoteStockFragmentViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<GameData>> gameDataList;
    private ArrayList<GameData> gameDataArrayList;

    public ObservableField<GameStockActivity> activityObservableField =
            new ObservableField<>();

    public MutableLiveData<ArrayList<GameData>> getGameData() {
        return gameDataList;
    }

    public RemoteStockFragmentViewModel () {
        gameDataList = new MutableLiveData<>();
        init();
    }

    private void init () {
        populateList();
        gameDataList.setValue(gameDataArrayList);
    }

    private void populateList () {
        GameStockActivity activity = new GameStockActivity();
        ArrayList<GameData> tempArrayList = activity.getSortedGames();
        for (GameData data : tempArrayList) {
            if (data.hasRemoteUrl()) {
                gameDataArrayList.add(data);
            }
        }
        gameDataArrayList = activity.getSortedGames();
    }
}
