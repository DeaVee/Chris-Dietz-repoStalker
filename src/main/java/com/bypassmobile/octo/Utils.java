package com.bypassmobile.octo;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;

/**
 *
 */
public class Utils {
    private static final int MAX_MEM_CACHE_SIZE = 30 * 1024 * 1024; // 30MB

    public static int calculateMemoryCacheSizeBytes(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            memoryClass = am.getLargeMemoryClass();
        }
        // Target 15% of the available RAM.
        int size = 1024 * 1024 * memoryClass / 7;
        // Bound to max size for mem cache.
        return Math.min(size, MAX_MEM_CACHE_SIZE);
    }
}
