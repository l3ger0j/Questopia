package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ListGameItemBindingImpl extends ListGameItemBinding  {

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
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ListGameItemBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 3, sIncludes, sViewsWithIds));
    }
    private ListGameItemBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (com.google.android.material.imageview.ShapeableImageView) bindings[1]
            , (android.widget.TextView) bindings[2]
            , (androidx.constraintlayout.widget.ConstraintLayout) bindings[0]
            );
        this.itemIcon.setTag(null);
        this.itemText.setTag(null);
        this.relativeLayout.setTag(null);
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
        if (BR.qpListItem == variableId) {
            setQpListItem((org.qp.android.model.libQSP.QpListItem) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setQpListItem(@Nullable org.qp.android.model.libQSP.QpListItem QpListItem) {
        this.mQpListItem = QpListItem;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.qpListItem);
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
        java.lang.String qpListItemPathToImage = null;
        java.lang.CharSequence qpListItemText = null;
        int textUtilsIsEmptyQpListItemPathToImageViewGONEViewVISIBLE = 0;
        org.qp.android.model.libQSP.QpListItem qpListItem = mQpListItem;
        boolean textUtilsIsEmptyQpListItemPathToImage = false;
        boolean textUtilsIsEmptyQpListItemText = false;
        int textUtilsIsEmptyQpListItemTextViewGONEViewVISIBLE = 0;

        if ((dirtyFlags & 0x3L) != 0) {



                if (qpListItem != null) {
                    // read qpListItem.pathToImage
                    qpListItemPathToImage = qpListItem.pathToImage;
                    // read qpListItem.text
                    qpListItemText = qpListItem.text;
                }


                // read TextUtils.isEmpty(qpListItem.pathToImage)
                textUtilsIsEmptyQpListItemPathToImage = android.text.TextUtils.isEmpty(qpListItemPathToImage);
                // read TextUtils.isEmpty(qpListItem.text)
                textUtilsIsEmptyQpListItemText = android.text.TextUtils.isEmpty(qpListItemText);
            if((dirtyFlags & 0x3L) != 0) {
                if(textUtilsIsEmptyQpListItemPathToImage) {
                        dirtyFlags |= 0x8L;
                }
                else {
                        dirtyFlags |= 0x4L;
                }
            }
            if((dirtyFlags & 0x3L) != 0) {
                if(textUtilsIsEmptyQpListItemText) {
                        dirtyFlags |= 0x20L;
                }
                else {
                        dirtyFlags |= 0x10L;
                }
            }


                // read TextUtils.isEmpty(qpListItem.pathToImage) ? View.GONE : View.VISIBLE
                textUtilsIsEmptyQpListItemPathToImageViewGONEViewVISIBLE = ((textUtilsIsEmptyQpListItemPathToImage) ? (android.view.View.GONE) : (android.view.View.VISIBLE));
                // read TextUtils.isEmpty(qpListItem.text) ? View.GONE : View.VISIBLE
                textUtilsIsEmptyQpListItemTextViewGONEViewVISIBLE = ((textUtilsIsEmptyQpListItemText) ? (android.view.View.GONE) : (android.view.View.VISIBLE));
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            this.itemIcon.setVisibility(textUtilsIsEmptyQpListItemPathToImageViewGONEViewVISIBLE);
            androidx.databinding.adapters.TextViewBindingAdapter.setText(this.itemText, qpListItemText);
            this.itemText.setVisibility(textUtilsIsEmptyQpListItemTextViewGONEViewVISIBLE);
        }
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): qpListItem
        flag 1 (0x2L): null
        flag 2 (0x3L): TextUtils.isEmpty(qpListItem.pathToImage) ? View.GONE : View.VISIBLE
        flag 3 (0x4L): TextUtils.isEmpty(qpListItem.pathToImage) ? View.GONE : View.VISIBLE
        flag 4 (0x5L): TextUtils.isEmpty(qpListItem.text) ? View.GONE : View.VISIBLE
        flag 5 (0x6L): TextUtils.isEmpty(qpListItem.text) ? View.GONE : View.VISIBLE
    flag mapping end*/
    //end
}