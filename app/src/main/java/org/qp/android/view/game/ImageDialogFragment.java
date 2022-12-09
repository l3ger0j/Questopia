package org.qp.android.view.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import org.qp.android.databinding.DialogImageBinding;

public class ImageDialogFragment extends DialogFragment {
    public ObservableField<String> pathToImage = new ObservableField<>();
    private DialogImageBinding imageBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        imageBinding = DialogImageBinding.inflate(getLayoutInflater());
        imageBinding.setDialogFragment(this);
        if (savedInstanceState != null && savedInstanceState.containsKey("pathToImage")) {
            pathToImage.set(savedInstanceState.getString("pathToImage"));
        }
        return imageBinding.getRoot();
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
