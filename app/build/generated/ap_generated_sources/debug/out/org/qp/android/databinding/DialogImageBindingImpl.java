package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogImageBindingImpl extends DialogImageBinding implements org.qp.android.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = null;
    }
    // views
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback1;
    // values
    // listeners
    // Inverse Binding Event Handlers

    public DialogImageBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 1, sIncludes, sViewsWithIds));
    }
    private DialogImageBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 1
            , (android.widget.ImageView) bindings[0]
            );
        this.imageBox.setTag(null);
        setRootTag(root);
        // listeners
        mCallback1 = new org.qp.android.generated.callback.OnClickListener(this, 1);
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x4L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.dialogFragment == variableId) {
            setDialogFragment((org.qp.android.view.game.fragments.dialogs.GameDialogFrags) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setDialogFragment(@Nullable org.qp.android.view.game.fragments.dialogs.GameDialogFrags DialogFragment) {
        this.mDialogFragment = DialogFragment;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
        }
        notifyPropertyChanged(BR.dialogFragment);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeDialogFragmentPathToImage((androidx.databinding.ObservableField<java.lang.String>) object, fieldId);
        }
        return false;
    }
    private boolean onChangeDialogFragmentPathToImage(androidx.databinding.ObservableField<java.lang.String> DialogFragmentPathToImage, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        java.lang.String dialogFragmentPathToImageGet = null;
        org.qp.android.view.game.fragments.dialogs.GameDialogFrags dialogFragment = mDialogFragment;
        androidx.databinding.ObservableField<java.lang.String> dialogFragmentPathToImage = null;

        if ((dirtyFlags & 0x7L) != 0) {



                if (dialogFragment != null) {
                    // read dialogFragment.pathToImage
                    dialogFragmentPathToImage = dialogFragment.pathToImage;
                }
                updateRegistration(0, dialogFragmentPathToImage);


                if (dialogFragmentPathToImage != null) {
                    // read dialogFragment.pathToImage.get()
                    dialogFragmentPathToImageGet = dialogFragmentPathToImage.get();
                }
        }
        // batch finished
        if ((dirtyFlags & 0x4L) != 0) {
            // api target 1

            this.imageBox.setOnClickListener(mCallback1);
        }
        if ((dirtyFlags & 0x7L) != 0) {
            // api target 1

            org.qp.android.view.adapters.Bind.loadImage(this.imageBox, dialogFragmentPathToImageGet);
        }
    }
    // Listener Stub Implementations
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        // localize variables for thread safety
        // dialogFragment
        org.qp.android.view.game.fragments.dialogs.GameDialogFrags dialogFragment = mDialogFragment;
        // dialogFragment != null
        boolean dialogFragmentJavaLangObjectNull = false;



        dialogFragmentJavaLangObjectNull = (dialogFragment) != (null);
        if (dialogFragmentJavaLangObjectNull) {


            dialogFragment.dismiss();
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): dialogFragment.pathToImage
        flag 1 (0x2L): dialogFragment
        flag 2 (0x3L): null
    flag mapping end*/
    //end
}