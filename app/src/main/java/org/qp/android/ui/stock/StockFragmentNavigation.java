package org.qp.android.ui.stock;

import androidx.annotation.IdRes;
import androidx.appcompat.view.ActionMode;

import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.EventNavigation;

public interface StockFragmentNavigation {

    class ShowErrorDialog extends EventNavigation {
        private final String errorMessage;
        private final ErrorType errorType;

        public ShowErrorDialog(String errorMessage, ErrorType errorType) {
            this.errorMessage = errorMessage;
            this.errorType = errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public ErrorType getErrorType() {
            return errorType;
        }
    }

    class ShowDeleteDialog extends EventNavigation {
        public final String errorMessage;

        public ShowDeleteDialog(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    class ShowActionMode extends EventNavigation {
        public final ActionMode.Callback callback;

        public ShowActionMode(ActionMode.Callback callback) {
            this.callback = callback;
        }
    }

    class FinishActionMode extends EventNavigation {
    }

    class ChangeDestination extends EventNavigation {
        @IdRes
        public final int resId;

        public ChangeDestination(int resId) {
            this.resId = resId;
        }
    }

    class ChangeElementColorToDKGray extends EventNavigation {
    }

    class ChangeElementColorToLTGray extends EventNavigation {
    }

    class SelectOnce extends EventNavigation {
        public final int position;

        public SelectOnce(int position) {
            this.position = position;
        }
    }

    class UnselectOnce extends EventNavigation {
        public final int position;

        public UnselectOnce(int position) {
            this.position = position;
        }
    }

    class ShowFilePicker extends EventNavigation {
        private final int requestCode;
        private final String[] mimeTypes;

        public ShowFilePicker(int requestCode, String[] mimeTypes) {
            this.requestCode = requestCode;
            this.mimeTypes = mimeTypes;
        }

        public int getRequestCode() {
            return requestCode;
        }

        public String[] getMimeTypes() {
            return mimeTypes;
        }
    }

}
