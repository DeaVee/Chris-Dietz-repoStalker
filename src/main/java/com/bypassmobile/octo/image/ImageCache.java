package com.bypassmobile.octo.image;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import com.bypassmobile.octo.Utils;
import com.squareup.picasso.Cache;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageCache implements Cache {

    private final Map<String,Bitmap> cacheMap;
    private final Map<String, Long> timeMap;

    private final int maxSizeBytes;
    private final int timeout;

    private int currSizeBytes;

    /**
     * Default constructor.  Memory size will be determined by the memory available on the specific
     * device.
     * @param ctx
     *      App context
     */
    public ImageCache(Context ctx) {
        this(Utils.calculateMemoryCacheSizeBytes(ctx));
    }

    /**
     * Memory size with this constructor will be determined by the memory available on the specific device.
     *
     * @param ctx
     *      App Context
     * @param timeout
     *      Duration for how long an image should stay in the cache.
     *      A value less than or equal to 0 means the image will be held as long as possible.
     */
    public ImageCache(Context ctx, int timeout) {
        this(Utils.calculateMemoryCacheSizeBytes(ctx), timeout);
    }

    /**
     * Manual input for how much memory the cache should hold.
     * @param maxSizeBytes
     *      Maximum number of bytes the cache should hold.  Must be greater than 0.
     */
    public ImageCache(int maxSizeBytes) {
        this(maxSizeBytes, -1);
    }

    /**
     * Manual input for how much memory the cache should hold as well as the time to hold it.
     *
     * For efficiency reasons, the only time a bitmap will be removed is during a get or set call.
     *
     * @param maxSizeBytes
     *      Maximum number of bytes the cache should hold.  Must be greater than 0.
     * @param timeoutMs
     *      timeout in milliseconds for the cache to hold an image.
     *      A value less than or equal to 0 means the image will be held as long as possible.
     */
    public ImageCache(int maxSizeBytes, int timeoutMs) {
        if (maxSizeBytes <= 0) {
            throw new IllegalArgumentException("The maximum number of bytes the cache should hold should be greater than 0.");
        }
        this.maxSizeBytes = maxSizeBytes;
        this.currSizeBytes = 0;
        this.cacheMap = new LinkedHashMap<>(0, 1, true);
        this.timeMap = (timeoutMs > 0) ? new HashMap<String, Long>(0, 1) : null;
        this.timeout = timeoutMs;
    }

    @Override
    public Bitmap get(String stringResource) {
        if (stringResource == null) {
            throw new IllegalArgumentException("Cannot retrieve bitmap with null string resource.");
        }
        synchronized (cacheMap) {
            // Update timestamp if it's being touched.
            if (timeMap != null && timeMap.containsKey(stringResource)) {
                timeMap.put(stringResource, System.currentTimeMillis());
            }
            trimOldBitmaps();
            return cacheMap.get(stringResource);
        }
    }

    @Override
    public void set(String stringResource, Bitmap bitmap) {
        if (stringResource == null || bitmap == null) {
            throw new IllegalArgumentException("The String url and bitmap can not be null.");
        }

        final int newBitmapSize = getByteCount(bitmap);
        if (newBitmapSize >= maxSizeBytes) {
            // Bitmap is most certainly too big too be added, instead dump it so the other images don't get flushed.
            removeFromCache(stringResource);
            return;
        }

        synchronized (cacheMap) {
            int oldBitmapSize = (cacheMap.containsKey(stringResource)) ? getByteCount(cacheMap.get(stringResource)) : 0;
            currSizeBytes += newBitmapSize - oldBitmapSize;
            cacheMap.put(stringResource, bitmap);
            if (timeMap != null) {
                timeMap.put(stringResource, System.currentTimeMillis());
            }

            trimOldBitmaps(); // Remove the old bitmaps first

            if (currSizeBytes > maxSizeBytes) {
                String keyToRemove = cacheMap.keySet().iterator().next();
                removeFromCache(keyToRemove);
            }
        }
    }

    @Override
    public int size() {
        synchronized (cacheMap) {
            return cacheMap.size();
        }
    }

    @Override
    public int maxSize() {
        return maxSizeBytes;
    }

    @Override
    public void clear() {
        synchronized (cacheMap) {
            cacheMap.clear();
        }
    }

    /**
     * Explicitly remove a specific bitmap resource from the cache.
     *
     * @return
     *      If found, the bitmap that was removed.
     */
    public Bitmap removeFromCache(String stringResource) {
        if (stringResource == null) {
            return null;
        }

        Bitmap old;
        synchronized (cacheMap) {
            if (timeMap != null) {
                timeMap.remove(stringResource);
            }

            old = cacheMap.remove(stringResource);
            if (old != null) {
                currSizeBytes -= getByteCount(old);
            }
        }
        return old;
    }

    /**
     * Trims all old bitmaps from the mapping if there is a timeout.
     */
    private void trimOldBitmaps() {
        if (timeMap == null || timeout < 0) {
            // No timeouts
            return;
        }

        synchronized (cacheMap) {
            long currentTime = System.currentTimeMillis();
            HashSet<String> keysToRemove = new HashSet<>(timeMap.size());
            int sizeToRemove = 0;
            for (String key : timeMap.keySet()) {
                long timed = currentTime - timeMap.get(key);
                if ((timed > timeout)) {
                    keysToRemove.add(key);
                    sizeToRemove += getByteCount(cacheMap.get(key));
                }
            }
            currSizeBytes -= sizeToRemove;
            cacheMap.keySet().removeAll(keysToRemove);
            timeMap.keySet().removeAll(keysToRemove);
        }
    }

    /**
     * Return the current size of the cache in bytes.  Used primarily for testing purposes.
     */
    /* internal */ int getCurrSizeBytes() {
        return currSizeBytes;
    }

    private static int getByteCount(Bitmap bmp) {
        if (bmp == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bmp.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bmp.getByteCount();
        } else {
            return bmp.getWidth() * bmp.getHeight();
        }
    }
}
