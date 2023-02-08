package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityStockBindingImpl extends ActivityStockBinding implements org.qp.android.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.banner_view, 2);
        sViewsWithIds.put(R.id.stockFragContainer, 3);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback4;
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityStockBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 4, sIncludes, sViewsWithIds));
    }
    private ActivityStockBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 1
            , (com.zhpan.bannerview.BannerViewPager) bindings[2]
            , (com.google.android.material.floatingactionbutton.FloatingActionButton) bindings[1]
            , (android.widget.FrameLayout) bindings[3]
            );
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.stockFAB.setTag(null);
        setRootTag(root);
        // listeners
        mCallback4 = new org.qp.android.generated.callback.OnClickListener(this, 1);
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
        if (BR.stockVM == variableId) {
            setStockVM((org.qp.android.viewModel.ActivityStock) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setStockVM(@Nullable org.qp.android.viewModel.ActivityStock StockVM) {
        this.mStockVM = StockVM;
        synchronized(this) {
            mDirtyFlags |= 0x2L;
        }
        notifyPropertyChanged(BR.stockVM);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeStockVMIsShowDialog((androidx.databinding.ObservableBoolean) object, fieldId);
        }
        return false;
    }
    private boolean onChangeStockVMIsShowDialog(androidx.databinding.ObservableBoolean StockVMIsShowDialog, int fieldId) {
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
        boolean stockVMIsShowDialogGet = false;
        org.qp.android.viewModel.ActivityStock stockVM = mStockVM;
        int stockVMIsShowDialogViewGONEViewVISIBLE = 0;
        androidx.databinding.ObservableBoolean stockVMIsShowDialog = null;

        if ((dirtyFlags & 0x7L) != 0) {



                if (stockVM != null) {
                    // read stockVM.isShowDialog
                    stockVMIsShowDialog = stockVM.isShowDialog;
                }
                updateRegistration(0, stockVMIsShowDialog);


                if (stockVMIsShowDialog != null) {
                    // read stockVM.isShowDialog.get()
                    stockVMIsShowDialogGet = stockVMIsShowDialog.get();
                }
            if((dirtyFlags & 0x7L) != 0) {
                if(stockVMIsShowDialogGet) {
                        dirtyFlags |= 0x10L;
                }
                else {
                        dirtyFlags |= 0x8L;
                }
            }


                // read stockVM.isShowDialog.get() ? View.GONE : View.VISIBLE
                stockVMIsShowDialogViewGONEViewVISIBLE = ((stockVMIsShowDialogGet) ? (android.view.View.GONE) : (android.view.View.VISIBLE));
        }
        // batch finished
        if ((dirtyFlags & 0x4L) != 0) {
            // api target 1

            this.stockFAB.setOnClickListener(mCallback4);
        }
        if ((dirtyFlags & 0x7L) != 0) {
            // api target 1

            this.stockFAB.setVisibility(stockVMIsShowDialogViewGONEViewVISIBLE);
        }
    }
    // Listener Stub Implementations
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        // localize variables for thread safety
        // stockVM
        org.qp.android.viewModel.ActivityStock stockVM = mStockVM;
        // stockVM != null
        boolean stockVMJavaLangObjectNull = false;



        stockVMJavaLangObjectNull = (stockVM) != (null);
        if (stockVMJavaLangObjectNull) {


            stockVM.showDialogInstall();
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): stockVM.isShowDialog
        flag 1 (0x2L): stockVM
        flag 2 (0x3L): null
        flag 3 (0x4L): stockVM.isShowDialog.get() ? View.GONE : View.VISIBLE
        flag 4 (0x5L): stockVM.isShowDialog.get() ? View.GONE : View.VISIBLE
    flag mapping end*/
    //end
}