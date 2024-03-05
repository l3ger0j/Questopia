package org.qp.android.helpers.bus;

import androidx.lifecycle.Observer;

import java.util.HashSet;
import java.util.function.Consumer;

public class EventObserver implements Observer<EventNavigation> {

    private final Consumer<EventNavigation> handlerBlock;
    private final HashSet<String> executedEvents = new HashSet<>();

    public EventObserver(Consumer<EventNavigation> handlerBlock) {
        this.handlerBlock = handlerBlock;
    }

    /** Clear All Executed Events */
    public void clearExecutedEvents() {
        executedEvents.clear();
    }

    @Override
    public void onChanged(EventNavigation it) {
        if (it != null) {
            switch (it.getType()) {
                case EXECUTE_WITHOUT_LIMITS , WAIT_OBSERVER_IF_NEEDED -> {
                    if (!it.isHandled()) {
                        it.setHandled(true);
                        handlerBlock.accept(it);
                    }
                }
                case EXECUTE_ONCE , WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE -> {
                    if (!executedEvents.contains(it.getClass().getSimpleName())) {
                        if (!it.isHandled()) {
                            it.setHandled(true);
                            executedEvents.add(it.getClass().getSimpleName());
                            handlerBlock.accept(it);
                        }
                    }
                }
            }
        }
    }
}
