package org.qp.android.ui.game;

import org.qp.android.helpers.bus.NavigationEvent;
import org.qp.android.ui.dialogs.GameDialogFragBuilder;
import org.qp.android.ui.dialogs.GamePopupType;

public interface GameFragmentNavigation {

    class FinishActivity extends NavigationEvent {
    }

    class StartRWSave extends NavigationEvent {
        public final int slotAction;

        public StartRWSave(int slotAction) {
            this.slotAction = slotAction;
        }
    }

    class WarnUser extends NavigationEvent {
        public final int tabId;

        public WarnUser(int tabId) {
            this.tabId = tabId;
        }
    }

    class ShowDialog extends NavigationEvent {
        public final GameDialogFragBuilder buildDialog;

        public ShowDialog(GameDialogFragBuilder buildDialog) {
            this.buildDialog = buildDialog;
        }
    }

    class ShowPopup extends NavigationEvent {
        public final GamePopupType popupType;

        public ShowPopup(GamePopupType popupType) {
            this.popupType = popupType;
        }
    }

}
