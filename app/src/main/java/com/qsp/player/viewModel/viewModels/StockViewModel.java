package com.qsp.player.viewModel.viewModels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.qsp.player.dto.stock.GameData;

import java.util.List;

public class StockViewModel extends ViewModel {

    private final MutableLiveData<List<GameData>> data = new MutableLiveData<>();

    public MutableLiveData<List<GameData>> getData() {
        return data;
    }

    public StockViewModel () {
    }
}