package org.qp.android.ui.settings;

import static org.qp.android.helpers.utils.ColorUtil.getHexColor;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
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

        var text = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.colorForeground, text, true);
        var background = ResourcesCompat.getColor(getResources(), R.color.md_theme_surfaceVariant, requireContext().getTheme());

        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);

        var newsBinding = FragmentNewsBinding.inflate(getLayoutInflater());
        var css = String.format("""
                h1 { margin-left: 0px; font-size: 1.2em; }
                li { margin-left: 0px; }
                ul { padding-left: 2em; }
                body { color: %s; background-color: %s; }
                """, getHexColor(text.data), getHexColor(background));
        var changeLog = new ChangeLog(requireContext(), css);
        newsBinding.newsView.loadData(
                changeLog.getFullLog(),
                "text/html",
                null
        );

        return newsBinding.getRoot();
    }

}
