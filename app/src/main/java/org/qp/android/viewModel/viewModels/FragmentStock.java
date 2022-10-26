package org.qp.android.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.qp.android.dto.stock.GameData;
import org.qp.android.view.stock.StockActivity;

import java.util.ArrayList;

public class FragmentStock extends ViewModel {
    private final MutableLiveData<ArrayList<GameData>> gameDataList;

    public ObservableField<StockActivity> activityObservableField =
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
