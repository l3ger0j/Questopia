package org.qp.android.helpers.bus;

public class NavigationEvent {
    public boolean isHandled;
    public Events.Type type;

    public NavigationEvent() {
        this(false, Events.Type.EXECUTE_WITHOUT_LIMITS);
    }

    public NavigationEvent(boolean isHandled, Events.Type type) {
        this.isHandled = isHandled;
        this.type = type;
    }
}
