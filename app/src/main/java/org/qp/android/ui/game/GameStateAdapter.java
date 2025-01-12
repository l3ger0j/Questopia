package org.qp.android.ui.game;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GameStateAdapter extends FragmentStateAdapter {

    private final static int COUNT_FRAGMENTS = 3;

    public GameStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public int getItemCount() {
        return COUNT_FRAGMENTS;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return switch (position) {
            case 1 -> new GameObjectFragment();
            case 2 -> new GameVarsFragment();
            default -> new GameMainFragment();
        };
    }

}
