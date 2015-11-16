package com.bypassmobile.octo.image;

import android.content.Context;
import android.graphics.Bitmap;

import com.bypassmobile.octo.image.ImageCache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 *
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class ImageCacheTest {

    static final int MEGABYTE = 1024 * 1024;
    
    @Mock
    Context ctx;

    @Test
    public void singleImageCache() {
        int cacheSize = MEGABYTE;
        ImageCache cache = new ImageCache(cacheSize);
        assertThat(0, equalTo(0));
        assertThat(cache.get("TestBitmap1"), nullValue());

        final Bitmap testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8);
        cache.set("TestBitmap1", testBitmap);
        assertThat(cache.get("TestBitmap1"), notNullValue());
        assertThat(cache.get("TestBitmap1").equals(testBitmap), equalTo(true));
        assertThat(cache.getCurrSizeBytes(), equalTo(testBitmap.getByteCount()));

        final Bitmap twiceTheBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ALPHA_8);
        cache.set("TestBitmap1", twiceTheBitmap);
        assertThat(cache.get("TestBitmap1"), notNullValue());
        assertThat(cache.get("TestBitmap1").equals(twiceTheBitmap), equalTo(true));
        assertThat(cache.getCurrSizeBytes(), equalTo(twiceTheBitmap.getByteCount()));

        final Bitmap wayTooLargeBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8);
        cache.set("TestBitmap1", wayTooLargeBitmap);
        assertThat(cache.get("TestBitmap1"), nullValue());
    }

    @Test
    public void lruCacheDump() {
        int cacheSize = MEGABYTE * 4;
        ImageCache cache = new ImageCache(cacheSize);
        // making each bitmap a megabyte in size so cache should overflow after 5.
        Bitmap[] bmps = {
                Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(1024, 1024, Bitmap.Config.ALPHA_8)
        };

        int currSize = bmps[0].getByteCount();
        cache.set("TestBitmap1", bmps[0]);

        currSize += bmps[1].getByteCount();
        cache.set("TestBitmap2", bmps[1]);
        assertThat(cache.getCurrSizeBytes(), equalTo(currSize));

        currSize += bmps[2].getByteCount();
        cache.set("TestBitmap3", bmps[2]);
        assertThat(cache.getCurrSizeBytes(), equalTo(currSize));

        currSize += bmps[3].getByteCount();
        cache.set("TestBitmap4", bmps[3]);
        assertThat(cache.getCurrSizeBytes(), equalTo(currSize));

        currSize += bmps[3].getByteCount();
        cache.set("TestBitmap5", bmps[4]);
        assertThat(cache.getCurrSizeBytes(), not(currSize));
        assertThat(cache.getCurrSizeBytes(), equalTo(currSize - bmps[0].getByteCount()));
        assertThat(cache.get("TestBitmap1"), nullValue());
        assertThat(cache.get("TestBitmap2"), notNullValue());
        assertThat(cache.get("TestBitmap3"), notNullValue());
        assertThat(cache.get("TestBitmap4"), notNullValue());
        assertThat(cache.get("TestBitmap5"), notNullValue());

        cache.get("TestBitmap2"); // touch it

        cache.set("TestBitmap1", bmps[0]);

        assertThat(cache.get("TestBitmap1"), notNullValue());
        assertThat(cache.get("TestBitmap2"), notNullValue());
        assertThat(cache.get("TestBitmap3"), nullValue());
    }

    @Test
    public void removeResource() {
        int cacheSize = MEGABYTE;
        ImageCache cache = new ImageCache(cacheSize);
        Bitmap[] bmps = {
                Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(200, 200, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(300, 300, Bitmap.Config.ALPHA_8)
        };

        cache.set("TestBitmap1", bmps[0]);
        cache.set("TestBitmap2", bmps[1]);
        cache.set("TestBitmap3", bmps[2]);
        assertThat(cache.get("TestBitmap1"), notNullValue());
        assertThat(cache.get("TestBitmap2"), notNullValue());
        assertThat(cache.get("TestBitmap3"), notNullValue());
        assertThat(cache.getCurrSizeBytes(), equalTo(bmps[0].getByteCount() + bmps[1].getByteCount() + bmps[2].getByteCount()));

        Bitmap removedBitmap = cache.removeFromCache("doesnotexist");
        assertThat(removedBitmap, nullValue());

        removedBitmap = cache.removeFromCache("TestBitmap2");
        assertThat(removedBitmap, notNullValue());
        assertThat(removedBitmap.equals(bmps[1]), equalTo(true));
        assertThat(cache.getCurrSizeBytes(), equalTo(bmps[0].getByteCount() + bmps[2].getByteCount()));
    }

    @Test
    public void testTimeout() {
        int cacheSize = MEGABYTE;
        int maxTime = 1000;

        final ImageCache cache = new ImageCache(cacheSize, maxTime);
        Bitmap[] bmps = {
                Bitmap.createBitmap(100, 100, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(200, 200, Bitmap.Config.ALPHA_8),
                Bitmap.createBitmap(300, 300, Bitmap.Config.ALPHA_8)
        };

        cache.set("TestBitmap1", bmps[0]);
        sleep(maxTime + maxTime/2);
        assertThat(cache.get("TestBitmap1"), notNullValue());

        cache.set("TestBitmap2", bmps[1]);
        sleep(maxTime + maxTime/2);
        assertThat(cache.get("TestBitmap2"), notNullValue());
        assertThat(cache.get("TestBitmap1"), nullValue());
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            // don't care
        }
    }
}
