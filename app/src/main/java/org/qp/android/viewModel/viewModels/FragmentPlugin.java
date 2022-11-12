package org.qp.android.viewModel.viewModels;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.qp.android.databinding.DialogPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;
import org.qp.android.model.plugin.PluginClient;
import org.qp.android.utils.ViewUtil;
import org.qp.android.view.plugin.PluginFragment;

import java.util.ArrayList;
import java.util.Objects;

public class FragmentPlugin extends ViewModel {
    private final MutableLiveData<ArrayList<PluginInfo>> pluginList;
    private androidx.appcompat.app.AlertDialog dialog;
    private DialogPluginBinding pluginBinding;
    private final PluginClient pluginClient = new PluginClient();
    private final ArrayList<PluginInfo> tempPluginInfo = new ArrayList<>();

    public ObservableField<PluginFragment> fragmentObservableField =
            new ObservableField<>();
    public ObservableBoolean isShowDialog = new ObservableBoolean();

    public FragmentPlugin() {
        pluginList = new MutableLiveData<>();
    }

    public MutableLiveData<ArrayList<PluginInfo>> getGameData() {
        return pluginList;
    }

    public void setGameDataArrayList(ArrayList<PluginInfo> gameDataArrayList) {
        pluginList.postValue(gameDataArrayList);
    }

    public void addPlugin() {
        String text = String.valueOf(Objects.requireNonNull(pluginBinding.textInputLayout.getEditText()).getText());
        pluginClient.connectPlugin(formingPluginDialog().getContext(), text);
        try {
            tempPluginInfo.add(pluginClient.getPluginInfo());
            setGameDataArrayList(tempPluginInfo);
        } catch (NullPointerException exception) {
            ViewUtil.showErrorDialog(
                    Objects.requireNonNull(fragmentObservableField.get()).requireContext(),
                    "There is no such plugin");
        }
        isShowDialog.set(false);
        dialog.dismiss();
    }

    public void showAddPluginDialog () {
        dialog = createAlertDialog(formingPluginDialog());
        dialog.show();
        isShowDialog.set(true);
    }

    @NonNull
    private View formingPluginDialog() {
        pluginBinding = DialogPluginBinding.inflate(LayoutInflater.from(
                Objects.requireNonNull(fragmentObservableField.get()).requireContext()));
        pluginBinding.setPluginVM(this);
        return pluginBinding.getRoot();
    }

    @NonNull
    private androidx.appcompat.app.AlertDialog createAlertDialog (View view) {
        MaterialAlertDialogBuilder dialogBuilder =
                new MaterialAlertDialogBuilder(
                        Objects.requireNonNull(fragmentObservableField.get()).requireContext());
        dialogBuilder.setOnCancelListener(dialogInterface -> isShowDialog.set(false));
        dialogBuilder.setView(view);
        return dialogBuilder.create();
    }
}
