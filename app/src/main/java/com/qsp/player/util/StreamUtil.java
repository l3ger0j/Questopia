package com.qsp.player.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamUtil {
    private static final int BUFFER_SIZE = 8192;

    /**
     * Copies stream <code>from</code> contents into the stream <code>to</code>.
     */
    public static void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) > 0) {
            to.write(buffer, 0, bytesRead);
        }
    }
}
