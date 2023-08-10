package org.qp.android.ui.news;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import org.qp.android.R;
import org.qp.android.databinding.FragmentNewsBinding;
import org.qp.android.helpers.adapters.AutoScrollRunnable;

import info.hannes.changelog.ChangeLog;

public class NewsFragment extends Fragment {

    private FragmentNewsBinding newsBinding;
    private AutoScrollRunnable autoScrollRunnable;
    private ViewPager2 bannerViewPager;

    @Override
    public void onResume() {
        super.onResume();
        bannerViewPager.postDelayed(autoScrollRunnable, 3000);
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerViewPager.removeCallbacks(autoScrollRunnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.newsMenuTitle);

        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.settingsFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);

        newsBinding = FragmentNewsBinding.inflate(getLayoutInflater());
        bannerViewPager = newsBinding.bannerView;

        var log = new ChangeLog(requireContext());
        newsBinding.newsView.loadData(log.getFullLog() , "text/html" , null);
        newsBinding.newsView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray , requireActivity().getTheme()));

        var fragmentAdapter = new NewsAdapterFragment(requireActivity());
        bannerViewPager.setAdapter(fragmentAdapter);
        autoScrollRunnable = new AutoScrollRunnable(bannerViewPager, 3000, false);

        return newsBinding.getRoot();
    }

}
