package org.qp.android.helpers.bus;

public class EventNavigation {

    private boolean isHandled;
    private EventType type;

    public boolean isHandled() {
        return isHandled;
    }

    public void setHandled(boolean handled) {
        isHandled = handled;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }
}
