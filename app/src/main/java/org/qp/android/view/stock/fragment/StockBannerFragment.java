package org.qp.android.view.stock.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.qp.android.R;
import org.qp.android.databinding.ListItemBannerBinding;

public class StockBannerFragment extends StockPatternFragment{
    private ListItemBannerBinding binding;
    private int pageNumber;

    @NonNull
    public static StockBannerFragment newInstance(int numPage) {
        var fragment = new StockBannerFragment();
        var args = new Bundle();
        args.putInt("num", numPage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments() != null ? getArguments().getInt("num") : 1;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        binding = ListItemBannerBinding.inflate(getLayoutInflater());
        if (pageNumber == 0) {
            binding.bannerImage.setImageResource(R.drawable.banner_0);
            binding.bannerImage.setOnClickListener(v -> {
                var intentImg0 = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://t.me/joinchat/AAAAAFgqAMXq0SA34umFbQ"));
                intentImg0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentImg0);
            });
        } else {
            binding.bannerImage.setImageResource(R.drawable.banner_1);
            binding.bannerImage.setOnClickListener(v -> {
                var intentImg1 = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://schoollife.fludilka.su/viewtopic.php"));
                intentImg1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentImg1);
            });
        }
        return binding.getRoot();
    }
}
