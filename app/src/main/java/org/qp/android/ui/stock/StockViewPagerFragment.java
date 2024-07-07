package org.qp.android.ui.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayoutMediator;

import org.qp.android.databinding.FragmentViewPagerBinding;

public class StockViewPagerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        var binding = FragmentViewPagerBinding.inflate(inflater, container, false);

        var stockPager = binding.stockPager;
        stockPager.setAdapter(new StockStateAdapter(getChildFragmentManager(), getLifecycle()));

        var tabLayout = binding.stockTabLayout;
        var tabLayoutMediator = new TabLayoutMediator(tabLayout , stockPager , (tab , position) ->
                tab.setText(position == 0 ? "Local game" : "Remote game"));
        tabLayoutMediator.attach();

        return binding.getRoot();
    }
}
