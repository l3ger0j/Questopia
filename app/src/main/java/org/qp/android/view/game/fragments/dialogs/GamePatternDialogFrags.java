package org.qp.android.view.game.fragments.dialogs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public abstract class GamePatternDialogFrags extends DialogFragment {

    public interface GamePatternDialogList {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
        void onDialogListClick(DialogFragment dialog, int which);
    }

    public GamePatternDialogFrags.GamePatternDialogList listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (GamePatternDialogFrags.GamePatternDialogList) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PatternDialogListener");
        }
    }
}
