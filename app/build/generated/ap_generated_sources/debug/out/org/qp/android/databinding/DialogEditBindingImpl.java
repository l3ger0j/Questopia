package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogEditBindingImpl extends DialogEditBinding implements org.qp.android.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.ET0, 5);
        sViewsWithIds.put(R.id.ET1, 6);
        sViewsWithIds.put(R.id.ET2, 7);
        sViewsWithIds.put(R.id.linearLayout4, 8);
        sViewsWithIds.put(R.id.modTV, 9);
        sViewsWithIds.put(R.id.linearLayout, 10);
        sViewsWithIds.put(R.id.fileTV, 11);
        sViewsWithIds.put(R.id.linearLayout2, 12);
        sViewsWithIds.put(R.id.imageView, 13);
        sViewsWithIds.put(R.id.imageTV, 14);
    }
    // views
    @NonNull
    private final android.widget.ScrollView mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback2;
    // values
    // listeners
    private OnClickListenerImpl mStockVMSendIntentAndroidViewViewOnClickListener;
    // Inverse Binding Event Handlers

    public DialogEditBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 15, sIncludes, sViewsWithIds));
    }
    private DialogEditBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (com.google.android.material.textfield.TextInputLayout) bindings[5]
            , (com.google.android.material.textfield.TextInputLayout) bindings[6]
            , (com.google.android.material.textfield.TextInputLayout) bindings[7]
            , (android.widget.Button) bindings[3]
            , (android.widget.Button) bindings[1]
            , (android.widget.Button) bindings[2]
            , (android.widget.Button) bindings[4]
            , (android.widget.TextView) bindings[11]
            , (android.widget.TextView) bindings[14]
            , (android.widget.ImageView) bindings[13]
            , (android.widget.LinearLayout) bindings[10]
            , (android.widget.LinearLayout) bindings[12]
            , (android.widget.LinearLayout) bindings[8]
            , (android.widget.TextView) bindings[9]
            );
        this.buttonSelectIcon.setTag(null);
        this.buttonSelectMod.setTag(null);
        this.buttonSelectPath.setTag(null);
        this.editBT.setTag(null);
        this.mboundView0 = (android.widget.ScrollView) bindings[0];
        this.mboundView0.setTag(null);
        setRootTag(root);
        // listeners
        mCallback2 = new org.qp.android.generated.callback.OnClickListener(this, 1);
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
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.stockVM);
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
        android.view.View.OnClickListener stockVMSendIntentAndroidViewViewOnClickListener = null;
        org.qp.android.viewModel.ActivityStock stockVM = mStockVM;

        if ((dirtyFlags & 0x3L) != 0) {



                if (stockVM != null) {
                    // read stockVM::sendIntent
                    stockVMSendIntentAndroidViewViewOnClickListener = (((mStockVMSendIntentAndroidViewViewOnClickListener == null) ? (mStockVMSendIntentAndroidViewViewOnClickListener = new OnClickListenerImpl()) : mStockVMSendIntentAndroidViewViewOnClickListener).setValue(stockVM));
                }
        }
        // batch finished
        if ((dirtyFlags & 0x3L) != 0) {
            // api target 1

            this.buttonSelectIcon.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
            this.buttonSelectMod.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
            this.buttonSelectPath.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
        }
        if ((dirtyFlags & 0x2L) != 0) {
            // api target 1

            this.editBT.setOnClickListener(mCallback2);
        }
    }
    // Listener Stub Implementations
    public static class OnClickListenerImpl implements android.view.View.OnClickListener{
        private org.qp.android.viewModel.ActivityStock value;
        public OnClickListenerImpl setValue(org.qp.android.viewModel.ActivityStock value) {
            this.value = value;
            return value == null ? null : this;
        }
        @Override
        public void onClick(android.view.View arg0) {
            this.value.sendIntent(arg0); 
        }
    }
    // callback impls
    public final void _internalCallbackOnClick(int sourceId , android.view.View callbackArg_0) {
        // localize variables for thread safety
        // stockVM
        org.qp.android.viewModel.ActivityStock stockVM = mStockVM;
        // stockVM != null
        boolean stockVMJavaLangObjectNull = false;



        stockVMJavaLangObjectNull = (stockVM) != (null);
        if (stockVMJavaLangObjectNull) {


            stockVM.createEditIntent();
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): stockVM
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}