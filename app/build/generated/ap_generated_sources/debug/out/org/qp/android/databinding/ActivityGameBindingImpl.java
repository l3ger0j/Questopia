package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityGameBindingImpl extends ActivityGameBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.toolbar, 3);
        sViewsWithIds.put(R.id.separator, 4);
        sViewsWithIds.put(R.id.actions, 5);
        sViewsWithIds.put(R.id.objects, 6);
    }
    // views
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityGameBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 7, sIncludes, sViewsWithIds));
    }
    private ActivityGameBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (androidx.recyclerview.widget.RecyclerView) bindings[5]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[0]
            , (android.webkit.WebView) bindings[1]
            , (androidx.recyclerview.widget.RecyclerView) bindings[6]
            , (android.view.View) bindings[4]
            , (androidx.appcompat.widget.Toolbar) bindings[3]
            , (android.webkit.WebView) bindings[2]
            );
        this.layoutTop.setTag(null);
        this.mainDesc.setTag(null);
        this.varsDesc.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
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
        if (BR.gameViewModel == variableId) {
            setGameViewModel((org.qp.android.viewModel.ActivityGame) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setGameViewModel(@Nullable org.qp.android.viewModel.ActivityGame GameViewModel) {
        this.mGameViewModel = GameViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.gameViewModel);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
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
        org.qp.android.viewModel.ActivityGame gameViewModel = mGameViewModel;
        android.webkit.WebViewClient gameViewModelWebViewClient = null;

        if ((dirtyFlags & 0x3L) != 0) {



                if (gameViewModel != null) {
                    // read gameViewModel.webViewClient
                    gameViewModelWebViewClient = gameViewModel.getWebViewClient();
                }
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            org.qp.android.view.adapters.Bind.setWebViewClient(this.mainDesc, gameViewModelWebViewClient);
            org.qp.android.view.adapters.Bind.setWebViewClient(this.varsDesc, gameViewModelWebViewClient);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): gameViewModel
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}