package org.qp.android.presentation.game;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GameStateAdapter extends FragmentStateAdapter {

    private final static int COUNT_FRAGMENTS = 4;

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
            case 0 -> new GameMainFragment();
            case 1 -> new GameObjectFragment();
            case 2 -> new GameVarsFragment();
            case 3 -> new GameUserInputFragment();
            default -> throw new IllegalStateException("Unexpected value: " + position);
        };
    }

}
