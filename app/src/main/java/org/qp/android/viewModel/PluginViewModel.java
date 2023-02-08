package org.qp.android.viewModel;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import org.qp.android.view.plugin.PluginFragment;

public class PluginViewModel extends ViewModel {
    public ObservableField<PluginFragment> fragmentObservableField =
            new ObservableField<>();

}