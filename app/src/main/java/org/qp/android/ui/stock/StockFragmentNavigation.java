package org.qp.android.ui.stock;

import org.qp.android.helpers.bus.EventNavigation;

public interface StockFragmentNavigation {

    class ShowErrorDialog extends EventNavigation {
        private final String errorMessage;
        private final ErrorType errorType;

        public ShowErrorDialog(String errorMessage , ErrorType errorType) {
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

    class ShowGameFragment extends EventNavigation {
        private final int position;

        public ShowGameFragment(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }
    }

    class ShowActionMode extends EventNavigation {}

    class ShowFilePicker extends EventNavigation {
        private final int requestCode;
        private final String[] mimeTypes;

        public ShowFilePicker(int requestCode , String[] mimeTypes) {
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
