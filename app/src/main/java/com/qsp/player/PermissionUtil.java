package com.qsp.player;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collection;

public class PermissionUtil {

    /**
     * Checks if all <code>permissions</code> are granted and requests denied permissions.
     * @return <code>true</code> if all permissions are granted, <code>false</code> otherwise
     */
    public static boolean requestPermissionsIfNotGranted(Activity activity, int requestCode, String... permissions) {
        Collection<String> deniedPermissions = new ArrayList<>();

        for (String perm : permissions) {
            boolean denied = ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_DENIED;
            if (denied) {
                deniedPermissions.add(perm);
            }
        }

        if (!deniedPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    deniedPermissions.toArray(new String[deniedPermissions.size()]),
                    requestCode);

            return false;
        }

        return true;
    }
}
