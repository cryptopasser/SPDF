package com.snakeway.pdfviewer.util;

import android.graphics.Bitmap;
import android.util.LruCache;

public class BitmapMemoryCacheHelper {
    /**
     * NotNull
     */
    private final LruCache<String, Bitmap> lruCache;


    public BitmapMemoryCacheHelper(int maxSizeRatio) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int maxSize = maxMemory / maxSizeRatio;
        lruCache = new LruCache<String, Bitmap>(maxSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
    }

    public void putBitmap(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            return;
        }
        lruCache.put(key, bitmap);
    }

    public Bitmap getBitmap(String key) {
        if (key == null) {
            return null;
        }
        return lruCache.get(key);
    }

    public void clearMemory(String key) {
        if (key == null) {
            return;
        }
        lruCache.remove(key);
    }

    public void clearAllMemory() {
        lruCache.evictAll();
    }
}
