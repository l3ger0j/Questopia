package org.qp.android.ui.stock;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.qp.android.databinding.FragmentViewPagerBinding;

public class StockViewPagerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var binding = FragmentViewPagerBinding.inflate(inflater, container, false);
        var stockViewModel = new ViewModelProvider(requireActivity()).get(StockViewModel.class);

        var stockPager = binding.stockPager;
        stockPager.setAdapter(new StockStateAdapter(getChildFragmentManager(), getLifecycle()));

        var tabLayout = binding.stockTabLayout;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                stockViewModel.currPageNumber.setValue(tab.getPosition());
                stockViewModel.refreshGameData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        var tabLayoutMediator = new TabLayoutMediator(tabLayout, stockPager, (tab , position) ->
                tab.setText(position == 0 ? "Local game" : "Remote game"));
        tabLayoutMediator.attach();

        return binding.getRoot();
    }
}