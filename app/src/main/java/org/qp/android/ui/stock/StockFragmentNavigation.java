package org.qp.android.ui.stock;

import org.qp.android.data.db.Game;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.NavigationEvent;

public interface StockFragmentNavigation {

    class ShowActionMode extends NavigationEvent {
    }

    class SelectAllElements extends NavigationEvent {
    }

    class UnselectAllElements extends NavigationEvent {
    }

    class ShowGameFragment extends NavigationEvent {
        public final Game entry;

        public ShowGameFragment(Game entryToShow) {
            this.entry = entryToShow;
        }
    }

    class ShowErrorDialog extends NavigationEvent {
        public final String errorMessage;
        public final ErrorType errorType;

        public ShowErrorDialog(String errorMessage, ErrorType errorType) {
            this.errorMessage = errorMessage;
            this.errorType = errorType;
        }
    }

    class ShowFilePicker extends NavigationEvent {
        public final int requestCode;
        public final String[] mimeTypes;

        public ShowFilePicker(int requestCode, String[] mimeTypes) {
            this.requestCode = requestCode;
            this.mimeTypes = mimeTypes;
        }
    }

    class ShowErrorBanner extends NavigationEvent {
        public final String inputMessage;
        public final String rightButtonMsg;

        public ShowErrorBanner(String inputMessage, String rightButtonMsg) {
            this.inputMessage = inputMessage;
            this.rightButtonMsg = rightButtonMsg;
        }
    }

}
