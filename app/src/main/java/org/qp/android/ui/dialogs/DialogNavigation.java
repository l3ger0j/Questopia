package org.qp.android.ui.dialogs;

import androidx.fragment.app.DialogFragment;

import org.qp.android.helpers.bus.EventNavigation;

public interface DialogNavigation {

    class DialogPositiveClick extends EventNavigation {
        private final DialogFragment fragment;

        public DialogPositiveClick(DialogFragment fragment) {
            this.fragment = fragment;
        }

        public DialogFragment getFragment() {
            return fragment;
        }
    }

    class DialogNegativeClick extends EventNavigation {
        private final DialogFragment fragment;

        public DialogNegativeClick(DialogFragment fragment) {
            this.fragment = fragment;
        }

        public DialogFragment getFragment() {
            return fragment;
        }
    }

    class DialogNeutralClick extends EventNavigation {
        private final DialogFragment fragment;

        public DialogNeutralClick(DialogFragment fragment) {
            this.fragment = fragment;
        }

        public DialogFragment getFragment() {
            return fragment;
        }
    }

    class DialogListClick extends EventNavigation {
        private final DialogFragment fragment;
        private final int which;

        public DialogListClick(DialogFragment fragment , int which) {
            this.fragment = fragment;
            this.which = which;
        }

        public DialogFragment getFragment() {
            return fragment;
        }

        public int getWhich() {
            return which;
        }
    }

    class DialogOnDestroy extends EventNavigation {
        private final DialogFragment fragment;

        public DialogOnDestroy(DialogFragment fragment) {
            this.fragment = fragment;
        }

        public DialogFragment getFragment() {
            return fragment;
        }

    }

}
