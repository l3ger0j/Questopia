package org.qp.android.databinding;
import org.qp.android.R;
import org.qp.android.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class DialogInstallBindingImpl extends DialogInstallBinding implements org.qp.android.generated.callback.OnClickListener.Listener {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.ET0, 7);
        sViewsWithIds.put(R.id.ET1, 8);
        sViewsWithIds.put(R.id.ET2, 9);
        sViewsWithIds.put(R.id.fileTV, 10);
        sViewsWithIds.put(R.id.folderTV, 11);
        sViewsWithIds.put(R.id.linearLayout2, 12);
        sViewsWithIds.put(R.id.imageView, 13);
        sViewsWithIds.put(R.id.imageTV, 14);
    }
    // views
    @NonNull
    private final android.widget.ScrollView mboundView0;
    // variables
    @Nullable
    private final android.view.View.OnClickListener mCallback3;
    // values
    // listeners
    private OnClickListenerImpl mStockVMSendIntentAndroidViewViewOnClickListener;
    // Inverse Binding Event Handlers

    public DialogInstallBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 15, sIncludes, sViewsWithIds));
    }
    private DialogInstallBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 2
            , (com.google.android.material.textfield.TextInputLayout) bindings[7]
            , (com.google.android.material.textfield.TextInputLayout) bindings[8]
            , (com.google.android.material.textfield.TextInputLayout) bindings[9]
            , (android.widget.Button) bindings[2]
            , (android.widget.Button) bindings[4]
            , (android.widget.Button) bindings[5]
            , (android.widget.TextView) bindings[10]
            , (android.widget.TextView) bindings[11]
            , (android.widget.TextView) bindings[14]
            , (android.widget.ImageView) bindings[13]
            , (android.widget.Button) bindings[6]
            , (android.widget.LinearLayout) bindings[1]
            , (android.widget.LinearLayout) bindings[3]
            , (android.widget.LinearLayout) bindings[12]
            );
        this.buttonSelectArchive.setTag(null);
        this.buttonSelectFolder.setTag(null);
        this.buttonSelectIcon.setTag(null);
        this.installBT.setTag(null);
        this.linearLayout.setTag(null);
        this.linearLayout1.setTag(null);
        this.mboundView0 = (android.widget.ScrollView) bindings[0];
        this.mboundView0.setTag(null);
        setRootTag(root);
        // listeners
        mCallback3 = new org.qp.android.generated.callback.OnClickListener(this, 1);
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x8L;
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
            mDirtyFlags |= 0x4L;
        }
        notifyPropertyChanged(BR.stockVM);
        super.requestRebind();
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
            case 0 :
                return onChangeStockVMIsSelectFolder((androidx.databinding.ObservableBoolean) object, fieldId);
            case 1 :
                return onChangeStockVMIsSelectArchive((androidx.databinding.ObservableBoolean) object, fieldId);
        }
        return false;
    }
    private boolean onChangeStockVMIsSelectFolder(androidx.databinding.ObservableBoolean StockVMIsSelectFolder, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x1L;
            }
            return true;
        }
        return false;
    }
    private boolean onChangeStockVMIsSelectArchive(androidx.databinding.ObservableBoolean StockVMIsSelectArchive, int fieldId) {
        if (fieldId == BR._all) {
            synchronized(this) {
                    mDirtyFlags |= 0x2L;
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
        boolean stockVMIsSelectArchiveGet = false;
        android.view.View.OnClickListener stockVMSendIntentAndroidViewViewOnClickListener = null;
        int stockVMIsSelectFolderViewGONEViewVISIBLE = 0;
        org.qp.android.viewModel.ActivityStock stockVM = mStockVM;
        int stockVMIsSelectArchiveViewGONEViewVISIBLE = 0;
        boolean stockVMIsSelectFolderGet = false;
        androidx.databinding.ObservableBoolean stockVMIsSelectFolder = null;
        androidx.databinding.ObservableBoolean stockVMIsSelectArchive = null;

        if ((dirtyFlags & 0xfL) != 0) {


            if ((dirtyFlags & 0xcL) != 0) {

                    if (stockVM != null) {
                        // read stockVM::sendIntent
                        stockVMSendIntentAndroidViewViewOnClickListener = (((mStockVMSendIntentAndroidViewViewOnClickListener == null) ? (mStockVMSendIntentAndroidViewViewOnClickListener = new OnClickListenerImpl()) : mStockVMSendIntentAndroidViewViewOnClickListener).setValue(stockVM));
                    }
            }
            if ((dirtyFlags & 0xdL) != 0) {

                    if (stockVM != null) {
                        // read stockVM.isSelectFolder
                        stockVMIsSelectFolder = stockVM.isSelectFolder;
                    }
                    updateRegistration(0, stockVMIsSelectFolder);


                    if (stockVMIsSelectFolder != null) {
                        // read stockVM.isSelectFolder.get()
                        stockVMIsSelectFolderGet = stockVMIsSelectFolder.get();
                    }
                if((dirtyFlags & 0xdL) != 0) {
                    if(stockVMIsSelectFolderGet) {
                            dirtyFlags |= 0x20L;
                    }
                    else {
                            dirtyFlags |= 0x10L;
                    }
                }


                    // read stockVM.isSelectFolder.get() ? View.GONE : View.VISIBLE
                    stockVMIsSelectFolderViewGONEViewVISIBLE = ((stockVMIsSelectFolderGet) ? (android.view.View.GONE) : (android.view.View.VISIBLE));
            }
            if ((dirtyFlags & 0xeL) != 0) {

                    if (stockVM != null) {
                        // read stockVM.isSelectArchive
                        stockVMIsSelectArchive = stockVM.isSelectArchive;
                    }
                    updateRegistration(1, stockVMIsSelectArchive);


                    if (stockVMIsSelectArchive != null) {
                        // read stockVM.isSelectArchive.get()
                        stockVMIsSelectArchiveGet = stockVMIsSelectArchive.get();
                    }
                if((dirtyFlags & 0xeL) != 0) {
                    if(stockVMIsSelectArchiveGet) {
                            dirtyFlags |= 0x80L;
                    }
                    else {
                            dirtyFlags |= 0x40L;
                    }
                }


                    // read stockVM.isSelectArchive.get() ? View.GONE : View.VISIBLE
                    stockVMIsSelectArchiveViewGONEViewVISIBLE = ((stockVMIsSelectArchiveGet) ? (android.view.View.GONE) : (android.view.View.VISIBLE));
            }
        }
        // batch finished
        if ((dirtyFlags & 0xcL) != 0) {
            // api target 1

            this.buttonSelectArchive.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
            this.buttonSelectFolder.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
            this.buttonSelectIcon.setOnClickListener(stockVMSendIntentAndroidViewViewOnClickListener);
        }
        if ((dirtyFlags & 0x8L) != 0) {
            // api target 1

            this.installBT.setOnClickListener(mCallback3);
        }
        if ((dirtyFlags & 0xdL) != 0) {
            // api target 1

            this.linearLayout.setVisibility(stockVMIsSelectFolderViewGONEViewVISIBLE);
        }
        if ((dirtyFlags & 0xeL) != 0) {
            // api target 1

            this.linearLayout1.setVisibility(stockVMIsSelectArchiveViewGONEViewVISIBLE);
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


            stockVM.createInstallIntent();
        }
    }
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): stockVM.isSelectFolder
        flag 1 (0x2L): stockVM.isSelectArchive
        flag 2 (0x3L): stockVM
        flag 3 (0x4L): null
        flag 4 (0x5L): stockVM.isSelectFolder.get() ? View.GONE : View.VISIBLE
        flag 5 (0x6L): stockVM.isSelectFolder.get() ? View.GONE : View.VISIBLE
        flag 6 (0x7L): stockVM.isSelectArchive.get() ? View.GONE : View.VISIBLE
        flag 7 (0x8L): stockVM.isSelectArchive.get() ? View.GONE : View.VISIBLE
    flag mapping end*/
    //end
}