package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;
import com.qsp.player.view.activities.GameStockActivity;
import com.qsp.player.view.adapters.GamesRecyclerAdapter;
import com.qsp.player.view.adapters.RecyclerItemClickListener;

public class StockViewModel extends ViewModel {
    private MutableLiveData<GamesRecyclerAdapter> recyclerAdapter =
            new MutableLiveData<>();

    public ObservableField<GameStockActivity> activityObservableField =
            new ObservableField<>();

    public void setRecyclerAdapter (GamesRecyclerAdapter gamesRecyclerAdapter) {
        recyclerAdapter.setValue(gamesRecyclerAdapter);
    }

    public LiveData<GamesRecyclerAdapter> getRecyclerAdapter () {
        if (recyclerAdapter == null) {
            recyclerAdapter = new MutableLiveData<>();
        }
        return recyclerAdapter;
    }
}