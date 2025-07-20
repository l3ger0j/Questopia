package org.qp.android.presentation.stock;

import androidx.appcompat.view.ActionMode;

import org.qp.android.data.source.database.model.Game;
import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.NavigationEvent;

public interface StockFragmentNavigation {

    class PrepareActionMode extends NavigationEvent {
    }

    class ShowActionMode extends NavigationEvent {
        public final ActionMode.Callback callback;

        public ShowActionMode(ActionMode.Callback callback) {
            this.callback = callback;
        }
    }

    class FinishActionMode extends NavigationEvent {
    }

    class DestroyActionMode extends NavigationEvent {
    }

    class ClearSelectElements extends NavigationEvent{
    }

    class ChangeStateElements extends NavigationEvent {
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

    class ShowDeleteDialog extends NavigationEvent {
        public final String deleteMessage;

        public ShowDeleteDialog(String errorMessage) {
            this.deleteMessage = errorMessage;
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

    class ShowDirPicker extends NavigationEvent {
        public final int requestCode;

        public ShowDirPicker(int requestCode) {
            this.requestCode = requestCode;
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
