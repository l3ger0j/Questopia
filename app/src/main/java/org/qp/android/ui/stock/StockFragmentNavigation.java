package org.qp.android.ui.stock;

import androidx.annotation.IdRes;
import androidx.appcompat.view.ActionMode;

import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.NavigationEvent;

public interface StockFragmentNavigation {

    class ShowErrorDialog extends NavigationEvent {
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

    class ShowDeleteDialog extends NavigationEvent {
        public final String errorMessage;

        public ShowDeleteDialog(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    class ShowActionMode extends NavigationEvent {
        public final ActionMode.Callback callback;

        public ShowActionMode(ActionMode.Callback callback) {
            this.callback = callback;
        }
    }

    class FinishActionMode extends NavigationEvent {
    }

    class ChangeDestination extends NavigationEvent {
        @IdRes
        public final int resId;

        public ChangeDestination(int resId) {
            this.resId = resId;
        }
    }

    class ChangeElementColorToDKGray extends NavigationEvent {
    }

    class ChangeElementColorToLTGray extends NavigationEvent {
    }

    class SelectOnce extends NavigationEvent {
        public final int position;

        public SelectOnce(int position) {
            this.position = position;
        }
    }

    class UnselectOnce extends NavigationEvent {
        public final int position;

        public UnselectOnce(int position) {
            this.position = position;
        }
    }

    class ShowFilePicker extends NavigationEvent {
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
