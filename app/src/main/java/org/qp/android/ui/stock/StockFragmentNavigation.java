package org.qp.android.ui.stock;

import org.qp.android.helpers.bus.EventNavigation;

public interface StockFragmentNavigation {

    class ShowErrorDialog extends EventNavigation {
        private final String errorMessage;

        public ShowErrorDialog(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
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

    class ShowActionMode extends EventNavigation{}

}
