package org.qp.android.helpers.bus;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.HashSet;

public class Events {
    private Events() {}

    public static class EventObserver implements Observer<NavigationEvent> {
        private final EventHandler handlerBlock;
        private final HashSet<String> executedEvents = new HashSet<>();

        public EventObserver(EventHandler handlerBlock) {
            this.handlerBlock = handlerBlock;
        }

        public void clearExecutedEvents() {
            executedEvents.clear();
        }

        @Override
        public void onChanged(NavigationEvent it) {
            if (it != null) {
                switch (it.type) {
                    case EXECUTE_WITHOUT_LIMITS:
                    case WAIT_OBSERVER_IF_NEEDED:
                        if (!it.isHandled) {
                            it.isHandled = true;
                            handlerBlock.handle(it);
                        }
                        break;
                    case EXECUTE_ONCE:
                    case WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE:
                        if (!executedEvents.contains(it.getClass().getSimpleName())) {
                            if (!it.isHandled) {
                                it.isHandled = true;
                                executedEvents.add(it.getClass().getSimpleName());
                                handlerBlock.handle(it);
                            }
                        }
                        break;
                }
            }
        }
    }

    public static class Emitter extends MutableLiveData<NavigationEvent> {
        private final ArrayList<NavigationEvent> waitingEvents = new ArrayList<>();
        private boolean isActive = false;

        public void emitAndExecute(NavigationEvent event) {
            newEvent(event, Type.EXECUTE_WITHOUT_LIMITS);
        }

        public void emitAndExecuteOnce(NavigationEvent event) {
            newEvent(event, Type.EXECUTE_ONCE);
        }

        public void waitAndExecute(NavigationEvent event) {
            newEvent(event, Type.WAIT_OBSERVER_IF_NEEDED);
        }

        public void waitAndExecuteOnce(NavigationEvent event) {
            newEvent(event, Type.WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE);
        }

        public void clearWaitingEvents() {
            waitingEvents.clear();
        }

        @Override
        protected void onInactive() {
            isActive = false;
        }

        @Override
        protected void onActive() {
            isActive = true;
            ArrayList<NavigationEvent> postingEvents = new ArrayList<>();
            for (NavigationEvent event : waitingEvents) {
                if (hasObservers()) {
                    setValue(event);
                    postingEvents.add(event);
                }
            }
            waitingEvents.removeAll(postingEvents);
        }

        private void newEvent(NavigationEvent event, Type type) {
            event.type = type;
            setValue(switch (type) {
                case EXECUTE_WITHOUT_LIMITS, EXECUTE_ONCE -> hasObservers() ? event : null;
                case WAIT_OBSERVER_IF_NEEDED, WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE -> {
                    if (hasObservers() && isActive) {
                        yield event;
                    } else {
                        waitingEvents.add(event);
                        yield null;
                    }
                }
            });
        }
    }

    public enum Type {
        EXECUTE_WITHOUT_LIMITS,
        EXECUTE_ONCE,
        WAIT_OBSERVER_IF_NEEDED,
        WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE
    }

    @FunctionalInterface
    public interface EventHandler {
        void handle(NavigationEvent event);
    }
}


