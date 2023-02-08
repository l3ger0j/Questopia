package org.qp.android.view.filepicker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public abstract class FilePatternDialogFrag extends DialogFragment {

    public interface FilePatternDialogList {
        void onDialogListClick(String tag, int which);
    }

    public FilePatternDialogFrag.FilePatternDialogList listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (FilePatternDialogFrag.FilePatternDialogList) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PatternDialogListener");
        }
    }
}
