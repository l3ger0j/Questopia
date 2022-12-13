package org.qp.android.view.game;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ObservableField;

import org.qp.android.databinding.DialogImageBinding;
import org.qp.android.utils.PatternDialogFragment;

public class ImageDialogFragment extends PatternDialogFragment {
    public ObservableField<String> pathToImage = new ObservableField<>();
    private DialogImageBinding imageBinding;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        imageBinding = DialogImageBinding.inflate(getLayoutInflater());
        imageBinding.setDialogFragment(this);
        if (savedInstanceState != null && savedInstanceState.containsKey("pathToImage")) {
            pathToImage.set(savedInstanceState.getString("pathToImage"));
        }
        return new AlertDialog.Builder(requireContext())
                .setView(imageBinding.getRoot())
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("pathToImage", pathToImage.get());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageBinding = null;
    }
}
