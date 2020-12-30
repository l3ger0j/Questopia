package com.qsp.player.libqsp.model;

public class RefreshInterfaceRequest {
    private boolean interfaceConfigChanged;
    private boolean mainDescChanged;
    private boolean actionsChanged;
    private boolean objectsChanged;
    private boolean varsDescChanged;

    public boolean isInterfaceConfigChanged() {
        return interfaceConfigChanged;
    }

    public void setInterfaceConfigChanged(boolean interfaceConfigChanged) {
        this.interfaceConfigChanged = interfaceConfigChanged;
    }

    public boolean isMainDescChanged() {
        return mainDescChanged;
    }

    public void setMainDescChanged(boolean mainDescChanged) {
        this.mainDescChanged = mainDescChanged;
    }

    public boolean isActionsChanged() {
        return actionsChanged;
    }

    public void setActionsChanged(boolean actionsChanged) {
        this.actionsChanged = actionsChanged;
    }

    public boolean isObjectsChanged() {
        return objectsChanged;
    }

    public void setObjectsChanged(boolean objectsChanged) {
        this.objectsChanged = objectsChanged;
    }

    public boolean isVarsDescChanged() {
        return varsDescChanged;
    }

    public void setVarsDescChanged(boolean varsDescChanged) {
        this.varsDescChanged = varsDescChanged;
    }
}
