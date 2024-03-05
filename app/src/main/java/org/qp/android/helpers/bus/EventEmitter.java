package org.qp.android.helpers.bus;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

public class EventEmitter extends MutableLiveData<EventNavigation> {

    private final ArrayList<EventNavigation> waitingEvents = new ArrayList<>();
    private boolean isActive = false;

    /** Clear All Waiting Events */
    public void clearWaitingEvents() {
        waitingEvents.clear();
    }

    // Default: Emit Event for Execution
    public void emitAndExecute(EventNavigation event) {
        newEvent(event, EventType.EXECUTE_WITHOUT_LIMITS);
    }

    // Emit Event for Execution Once
    public void emitAndExecuteOnce(EventNavigation event) {
        newEvent(event, EventType.EXECUTE_ONCE);
    }

    // Wait Observer Available and Emit Event for Execution
    public void waitAndExecute(EventNavigation event) {
        newEvent(event, EventType.WAIT_OBSERVER_IF_NEEDED);
    }

    // Wait Observer Available and Emit Event for Execution Once
    public void waitAndExecuteOnce(EventNavigation event) {
        newEvent(event, EventType.WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE);
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        isActive = false;
    }


    @Override
    protected void onActive() {
        super.onActive();
        isActive = true;
        var postingEvents = new ArrayList<EventNavigation>();
        waitingEvents.forEach(event -> {
            if (hasObservers()) {
                setValue(event);
                postingEvents.add(event);
            }
        });
        waitingEvents.removeAll(postingEvents);
    }


    private void newEvent(EventNavigation event, EventType type) {
        event.setType(type);
        switch (type) {
            case EXECUTE_WITHOUT_LIMITS , EXECUTE_ONCE -> setValue(hasObservers() ? event : null);
            case WAIT_OBSERVER_IF_NEEDED , WAIT_OBSERVER_IF_NEEDED_AND_EXECUTE_ONCE -> {
                if (hasObservers() && isActive) {
                    setValue(event);
                } else {
                    waitingEvents.add(event);
                    setValue(null);
                }
            }
        }
    }

}
