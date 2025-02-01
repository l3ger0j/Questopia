package org.qp.android.ui.game;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.qp.android.helpers.ErrorType;
import org.qp.android.helpers.bus.NavigationEvent;
import org.qp.android.ui.dialogs.GameDialogType;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public interface GameFragmentNavigation {

    class ApplySettings extends NavigationEvent {}

    class ShowPopupSave extends NavigationEvent {}

    class FinishActivity extends NavigationEvent {}

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

    class ShowMessageDialog extends NavigationEvent {
        public final String inputString;
        public final CountDownLatch latch;

        public ShowMessageDialog(@Nullable String inputString,
                                 @NonNull CountDownLatch latch) {
            this.inputString = inputString;
            this.latch = latch;
        }
    }

    class ShowInputDialog extends NavigationEvent {
        public final String inputString;
        public final ArrayBlockingQueue<String> inputQueue;

        public ShowInputDialog(@Nullable String inputString,
                               @NonNull ArrayBlockingQueue<String> inputQueue) {
            this.inputString = inputString;
            this.inputQueue = inputQueue;
        }
    }

    class ShowExecutorDialog extends NavigationEvent {
        public final String inputString;
        public final ArrayBlockingQueue<String> inputQueue;

        public ShowExecutorDialog(@Nullable String inputString,
                                  @NonNull ArrayBlockingQueue<String> inputQueue) {
            this.inputString = inputString;
            this.inputQueue = inputQueue;
        }
    }

    class ShowMenuDialog extends NavigationEvent {
        public final List<String> inputListString;
        public final ArrayBlockingQueue<Integer> inputQueue;

        public ShowMenuDialog(@Nullable List<String> inputListString,
                              @NonNull ArrayBlockingQueue<Integer> inputQueue) {
            this.inputListString = inputListString;
            this.inputQueue = inputQueue;
        }
    }

    class ShowSimpleDialog extends NavigationEvent {
        public final String inputString;
        public final GameDialogType dialogType;
        public final ErrorType errorType;

        public ShowSimpleDialog(@NonNull String inputString,
                                @NonNull GameDialogType dialogType,
                                @Nullable ErrorType errorType) {
            this.inputString = inputString;
            this.dialogType = dialogType;
            this.errorType = errorType;
        }
    }

}
