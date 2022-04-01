package com.snakeway.pdfviewer.model;

import android.graphics.Bitmap;

public class RenderedBitmap {
    private int width;
    private int height;
    private String base64;

    public RenderedBitmap(int width, int height, String base64) {
        super();
        this.width = width;
        this.height = height;
        this.base64 = base64;
    }

    @Override
    public String toString() {
        return "RenderedBitmap{" +
                "width=" + width +
                ", height=" + height +
                ", base64='" + base64 + '\'' +
                '}';
    }
}