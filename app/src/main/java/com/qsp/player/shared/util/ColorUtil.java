package com.qsp.player.shared.util;

import androidx.annotation.NonNull;

public final class ColorUtil {

    public static int convertRgbaToBgra(int color) {
        return 0xff000000 |
                ((color & 0x000000ff) << 16) |
                (color & 0x0000ff00) |
                ((color & 0x00ff0000) >> 16);
    }

    @NonNull
    public static String getHexColor(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
