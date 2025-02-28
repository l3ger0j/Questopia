package org.qp.android.helpers.utils;

import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class AccessibilityUtil {

    public static View.AccessibilityDelegate customAccessibilityDelegate() {
        return new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, @Nullable Bundle args) {
                if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                    return host.performClick();
                } else if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                    return host.performLongClick();
                } else {
                    return super.performAccessibilityAction(host, action, args);
                }
            }
        };
    }

}
