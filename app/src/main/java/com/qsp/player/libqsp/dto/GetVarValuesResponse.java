package com.qsp.player.libqsp.dto;

public class GetVarValuesResponse {
    private boolean success;
    private String stringValue;
    private int intValue;

    public boolean isSuccess() {
        return success;
    }

    public String getStringValue() {
        return stringValue;
    }

    public int getIntValue() {
        return intValue;
    }
}
