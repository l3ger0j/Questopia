package org.qp.android.presentation.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.qp.android.databinding.FragmentUserInputBinding;
import org.qp.android.questopiabundle.lib.LibGameRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GameUserInputFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final var viewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);
        final var textInputLayout = FragmentUserInputBinding.inflate(getLayoutInflater());
        textInputLayout.getRoot().setBackgroundColor(viewModel.getBackgroundColor());
        viewModel.controllerObserver.observe(getViewLifecycleOwner(), settingsController ->
                textInputLayout.getRoot().setBackgroundColor(viewModel.getBackgroundColor()));
        var editText = textInputLayout.gameInputLayout.getEditText();
        if (editText != null) {
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    var currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                    textInputLayout.gameInputText.append("\n"+currentTime+"|SEND SUCCESS! DEBUG TEXT:"+editText.getText());
                    if (viewModel.getSettingsController().isUseExecString) {
                        viewModel.requestForNativeLib(LibGameRequest.USE_EXECUTOR, String.valueOf(editText.getText()));
                    } else {
                        viewModel.requestForNativeLib(LibGameRequest.USE_INPUT, String.valueOf(editText.getText()));
                    }
                }
                return true;
            });
        }
        return textInputLayout.getRoot();
    }
}
