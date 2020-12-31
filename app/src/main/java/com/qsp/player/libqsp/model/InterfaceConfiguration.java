package com.qsp.player.libqsp.model;

public class InterfaceConfiguration {
    private boolean useHtml;
    private int fontSize;
    private int backColor;
    private int fontColor;
    private int linkColor;

    public void reset() {
        useHtml = false;
        fontSize = 0;
        backColor = 0;
        fontColor = 0;
        linkColor = 0;
    }

    public boolean isUseHtml() {
        return useHtml;
    }

    public void setUseHtml(boolean useHtml) {
        this.useHtml = useHtml;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public int getBackColor() {
        return backColor;
    }

    public void setBackColor(int backColor) {
        this.backColor = backColor;
    }

    public int getFontColor() {
        return fontColor;
    }

    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
    }

    public int getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(int linkColor) {
        this.linkColor = linkColor;
    }
}
