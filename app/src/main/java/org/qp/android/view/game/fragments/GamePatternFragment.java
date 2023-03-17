package org.qp.android.view.game.fragments;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class GamePatternFragment extends Fragment {
    public interface GamePatternFragmentList {
        void showPictureDialog (String pathToImg);
    }

    public GamePatternFragmentList listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (GamePatternFragmentList) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PatternDialogListener");
        }
    }
}

