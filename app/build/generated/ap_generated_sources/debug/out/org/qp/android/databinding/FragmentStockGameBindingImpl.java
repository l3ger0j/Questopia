package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class FragmentStockGameBindingImpl extends FragmentStockGameBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.constraintLayout, 7);
        sViewsWithIds.put(R.id.tableLayout, 8);
        sViewsWithIds.put(R.id.linearLayout3, 9);
        sViewsWithIds.put(R.id.editButton, 10);
        sViewsWithIds.put(R.id.playButton, 11);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    @NonNull
    private final android.widget.TextView mboundView5;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public FragmentStockGameBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 12, sIncludes, sViewsWithIds));
    }
    private FragmentStockGameBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[7]
            , (com.google.android.material.button.MaterialButton) bindings[10]
            , (android.widget.TextView) bindings[3]
            , (android.widget.ImageView) bindings[1]
            , (android.widget.TextView) bindings[2]
            , (android.widget.TextView) bindings[6]
            , (android.widget.LinearLayout) bindings[9]
            , (com.google.android.material.button.MaterialButton) bindings[11]
            , (android.widget.LinearLayout) bindings[8]
            , (android.widget.TextView) bindings[4]
            );
        this.gameAuthorTV.setTag(null);
        this.gameIconIV.setTag(null);
        this.gameTitleTV.setTag(null);
        this.gameVersionTV.setTag(null);
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.mboundView5 = (android.widget.TextView) bindings[5];
        this.mboundView5.setTag("gameType");
        this.textView5.setTag(null);
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
        if (BR.viewModel == variableId) {
            setViewModel((org.qp.android.viewModel.FragmentStockGame) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable org.qp.android.viewModel.FragmentStockGame ViewModel) {
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.viewModel);
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
        java.lang.String viewModelGameDataIcon = null;
        java.lang.String viewModelGetGameAuthor = null;
        java.lang.String viewModelGameDataTitle = null;
        java.lang.String viewModelGetGameType = null;
        java.lang.String viewModelGetGameVersion = null;
        java.lang.String viewModelGetGameSize = null;
        org.qp.android.dto.stock.GameData viewModelGameData = null;
        org.qp.android.viewModel.FragmentStockGame viewModel = mViewModel;

        if ((dirtyFlags & 0x3L) != 0) {



                if (viewModel != null) {
                    // read viewModel.getGameAuthor()
                    viewModelGetGameAuthor = viewModel.getGameAuthor();
                    // read viewModel.getGameType()
                    viewModelGetGameType = viewModel.getGameType();
                    // read viewModel.getGameVersion()
                    viewModelGetGameVersion = viewModel.getGameVersion();
                    // read viewModel.getGameSize()
                    viewModelGetGameSize = viewModel.getGameSize();
                    // read viewModel.gameData
                    viewModelGameData = viewModel.getGameData();
                }


                if (viewModelGameData != null) {
                    // read viewModel.gameData.icon
                    viewModelGameDataIcon = viewModelGameData.icon;
                    // read viewModel.gameData.title
                    viewModelGameDataTitle = viewModelGameData.title;
                }
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.gameAuthorTV, viewModelGetGameAuthor);
            org.qp.android.view.adapters.Bind.loadImage(this.gameIconIV, viewModelGameDataIcon);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.gameTitleTV, viewModelGameDataTitle);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.gameVersionTV, viewModelGetGameVersion);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.mboundView5, viewModelGetGameType);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.textView5, viewModelGetGameSize);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}