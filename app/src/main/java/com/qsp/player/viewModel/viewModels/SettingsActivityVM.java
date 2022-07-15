package com.qsp.player.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import com.qsp.player.view.activities.SettingsActivity;

public class SettingsActivityVM extends ViewModel {

    public ObservableField<SettingsActivity> settingsActivityObservableField =
            new ObservableField<>();

}