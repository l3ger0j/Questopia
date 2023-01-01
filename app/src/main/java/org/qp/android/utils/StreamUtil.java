package org.qp.android.utils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class StreamUtil {
    private static final int BUFFER_SIZE = 8192;

    /**
     * Copies data from the <code>from</code> stream to the <code>to</code> stream.
     */
    public static void copy(@NonNull InputStream from, OutputStream to) throws IOException {
        var buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = from.read(buffer)) > 0) {
            to.write(buffer, 0, bytesRead);
        }
    }
}
