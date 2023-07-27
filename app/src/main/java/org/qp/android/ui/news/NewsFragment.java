package org.qp.android.ui.news;

import android.content.Intent;
import android.net.Uri;
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

public class NewsFragment extends Fragment {

    private FragmentNewsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater ,
                             @Nullable ViewGroup container ,
                             @Nullable Bundle savedInstanceState) {
        requireActivity().setTitle("News Quest Player");

        var callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Navigation.findNavController(requireView()).navigate(R.id.settingsFragment);
            }
        };
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);

        binding = FragmentNewsBinding.inflate(getLayoutInflater());
        binding.bannerImage1.setOnClickListener(list -> {
            var intentImg0 = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/joinchat/AAAAAFgqAMXq0SA34umFbQ"));
            intentImg0.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentImg0);
        });
        binding.bannerImage2.setOnClickListener(l -> {
            var intentImg1 = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://schoollife.fludilka.su/viewtopic.php"));
            intentImg1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentImg1);
        });

        return binding.getRoot();
    }
}
