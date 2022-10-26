package org.qp.android.utils;

import android.os.Looper;

public final class ThreadUtil {

    /**
     * @return <code>true</code> если текущий поток - <code>thread</code>, иначе <code>false</code>
     */
    public static boolean isSameThread(Thread thread) {
        return Thread.currentThread().equals(thread);
    }

    /**
     * Throw <code>IllegalStateException</code>, if the method is called in the main thread
     */
    public static void assertNonUiThread() {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            throw new IllegalStateException("This should not be run on the UI thread");
        }
    }

    /**
     * @return <code>true</code> если текущий поток - основной, иначе <code>false</code>
     */
    public static boolean isMainThread() {
        return Thread.currentThread().equals(Looper.getMainLooper().getThread());
    }

    /**
     * Вбрасывает <code>RuntimeException</code>, если метод вызывается не из основного потока.
     */
    public static void throwIfNotMainThread() {
        if (!isMainThread()) {
            throw new RuntimeException("Must be called from the main thread");
        }
    }
}
