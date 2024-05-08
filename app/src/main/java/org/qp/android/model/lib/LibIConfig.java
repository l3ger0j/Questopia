package org.qp.android.model.lib;

public class LibIConfig {
    public boolean useHtml;
    public int fontSize;
    public int backColor;
    public int fontColor;
    public int linkColor;

    public void reset() {
        useHtml = false;
        fontSize = 0;
        backColor = 0;
        fontColor = 0;
        linkColor = 0;
    }
}
