package org.qp.android.viewModel.viewModels;

import androidx.databinding.ObservableField;
import androidx.lifecycle.ViewModel;

import org.qp.android.view.plugin.PluginFragment;

public class FragmentPlugin extends ViewModel {
    public ObservableField<PluginFragment> fragmentObservableField =
            new ObservableField<>();

}
