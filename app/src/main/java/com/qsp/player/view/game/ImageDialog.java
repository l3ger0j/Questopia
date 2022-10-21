package com.qsp.player.view.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.ObservableField;
import androidx.fragment.app.DialogFragment;

import com.qsp.player.databinding.DialogImageBinding;

public class ImageDialog extends DialogFragment {
    public ObservableField<String> pathToImage = new ObservableField<>();
    private DialogImageBinding imageBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        imageBinding = DialogImageBinding.inflate(getLayoutInflater());
        imageBinding.setDialogFragment(this);
        return imageBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        imageBinding = null;
    }
}
