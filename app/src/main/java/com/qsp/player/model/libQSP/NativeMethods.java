package com.qsp.player.model.libQSP;

import java.util.Objects;

/**
 * Методы данного класса определены в <code>androidqspwrapper.c</code>.
 */
public class NativeMethods {
    private final LibQspCallbacks callbacks;

    static {
        System.loadLibrary("ndkqsp");
    }

    public NativeMethods(LibQspCallbacks callbacks) {
        this.callbacks = Objects.requireNonNull(callbacks, "callbacks");
    }

    public native void QSPInit();
    public native void QSPDeInit();
    public native boolean QSPIsInCallBack();
    public native void QSPEnableDebugMode(boolean isDebug);
    public native Object QSPGetCurStateData();//!!!STUB
    public native String QSPGetVersion();
    public native int QSPGetFullRefreshCount();
    public native String QSPGetQstFullPath();
    public native String QSPGetCurLoc();
    public native String QSPGetMainDesc();
    public native boolean QSPIsMainDescChanged();
    public native String QSPGetVarsDesc();
    public native boolean QSPIsVarsDescChanged();
    public native Object QSPGetExprValue();//!!!STUB
    public native void QSPSetInputStrText(String val);
    public native int QSPGetActionsCount();
    public native Object QSPGetActionData(int ind);//!!!STUB
    public native boolean QSPExecuteSelActionCode(boolean isRefresh);
    public native boolean QSPSetSelActionIndex(int ind, boolean isRefresh);
    public native int QSPGetSelActionIndex();
    public native boolean QSPIsActionsChanged();
    public native int QSPGetObjectsCount();
    public native Object QSPGetObjectData(int ind);//!!!STUB
    public native boolean QSPSetSelObjectIndex(int ind, boolean isRefresh);
    public native int QSPGetSelObjectIndex();
    public native boolean QSPIsObjectsChanged();
    public native void QSPShowWindow(int type, boolean isShow);
    public native Object QSPGetVarValuesCount(String name);
    public native Object QSPGetVarValues(String name, int ind);//!!!STUB
    public native int QSPGetMaxVarsCount();
    public native Object QSPGetVarNameByIndex(int index);//!!!STUB
    public native boolean QSPExecString(String s, boolean isRefresh);
    public native boolean QSPExecLocationCode(String name, boolean isRefresh);
    public native boolean QSPExecCounter(boolean isRefresh);
    public native boolean QSPExecUserInput(boolean isRefresh);
    public native Object QSPGetLastErrorData();
    public native String QSPGetErrorDesc(int errorNum);
    public native boolean QSPLoadGameWorld(String fileName);
    public native boolean QSPLoadGameWorldFromData(byte[] data , int dataSize, String fileName);
    public native boolean QSPSaveGame(String fileName, boolean isRefresh);
    public native byte[] QSPSaveGameAsData(boolean isRefresh);
    public native boolean QSPOpenSavedGame(String fileName, boolean isRefresh);
    public native boolean QSPOpenSavedGameFromData(byte[] data , int dataSize, boolean isRefresh);
    public native boolean QSPRestartGame(boolean isRefresh);
    public native void QSPSelectMenuItem(int index);
    //public native void QSPSetCallBack(int type, QSP_CALLBACK func)
}
