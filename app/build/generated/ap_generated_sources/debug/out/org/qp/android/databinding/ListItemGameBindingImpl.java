package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ListItemGameBindingImpl extends ListItemGameBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.relativeLayout, 2);
        sViewsWithIds.put(R.id.game_icon, 3);
        sViewsWithIds.put(R.id.game_size, 4);
    }
    // views
    @NonNull
    private final androidx.cardview.widget.CardView mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ListItemGameBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds));
    }
    private ListItemGameBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (com.google.android.material.imageview.ShapeableImageView) bindings[3]
            , (android.widget.TextView) bindings[4]
            , (android.widget.TextView) bindings[1]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[2]
            );
        this.gameTitle.setTag(null);
        this.mboundView0 = (androidx.cardview.widget.CardView) bindings[0];
        this.mboundView0.setTag("gameCardView");
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
        if (BR.gameData == variableId) {
            setGameData((org.qp.android.dto.stock.GameData) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setGameData(@Nullable org.qp.android.dto.stock.GameData GameData) {
        this.mGameData = GameData;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.gameData);
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
        org.qp.android.dto.stock.GameData gameData = mGameData;
        java.lang.String gameDataTitle = null;
        boolean gameDataIsInstalled = false;
        int gameDataIsInstalledInt0xFFE0E0E0Int0xFFFFD700 = 0;

        if ((dirtyFlags & 0x3L) != 0) {



                if (gameData != null) {
                    // read gameData.title
                    gameDataTitle = gameData.title;
                    // read gameData.isInstalled
                    gameDataIsInstalled = gameData.isInstalled();
                }
            if((dirtyFlags & 0x3L) != 0) {
                if(gameDataIsInstalled) {
                        dirtyFlags |= 0x8L;
                }
                else {
                        dirtyFlags |= 0x4L;
                }
            }


                // read gameData.isInstalled ? 0xFFE0E0E0 : 0xFFFFD700
                gameDataIsInstalledInt0xFFE0E0E0Int0xFFFFD700 = ((gameDataIsInstalled) ? (0xFFE0E0E0) : (0xFFFFD700));
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.gameTitle, gameDataTitle);
            this.gameTitle.setTextColor(gameDataIsInstalledInt0xFFE0E0E0Int0xFFFFD700);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): gameData
        flag 1 (0x2L): null
        flag 2 (0x3L): gameData.isInstalled ? 0xFFE0E0E0 : 0xFFFFD700
        flag 3 (0x4L): gameData.isInstalled ? 0xFFE0E0E0 : 0xFFFFD700
    flag mapping end*/
    //end
}