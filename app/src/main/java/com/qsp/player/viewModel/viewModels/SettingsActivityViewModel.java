package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.qsp.player.view.activities.SettingsActivity;

public class SettingsActivityViewModel extends ViewModel {

    public ObservableField<SettingsActivity> settingsActivityObservableField =
            new ObservableField<>();

}