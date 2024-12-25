package org.qp.android.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import org.qp.android.R;
import org.qp.android.databinding.FragmentNewsBinding;

import info.hannes.changelog.ChangeLog;

public class SettingNewsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.settingsFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);

        var newsBinding = FragmentNewsBinding.inflate(getLayoutInflater());
        var changeLog = new ChangeLog(requireContext());
        newsBinding.newsView.loadData(
                changeLog.getFullLog(),
                "text/html",
                null
        );

        return newsBinding.getRoot();
    }

}
