package com.qsp.player.util;

import android.os.Looper;

public final class ThreadUtil {

    /**
     * @return <code>true</code> если текущий поток - основной, иначе <code>false</code>
     */
    public static boolean isThreadMain() {
        return Thread.currentThread().equals(Looper.getMainLooper().getThread());
    }

    /**
     * Вбрасывает <code>RuntimeException</code>, если метод вызывается не из основного потока.
     */
    public static void throwIfThreadIsNotMain() {
        if (!isThreadMain()) {
            throw new RuntimeException("Must be called from the main thread");
        }
    }
}
