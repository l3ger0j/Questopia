package org.qp.android.helpers.utils;

import android.net.Uri;

public final class UriUtil {

    public static boolean isNotEmptyOrBlanks(Uri uri) {
        return uri != null && uri != Uri.EMPTY && !uri.toString().isBlank();
    }

}
