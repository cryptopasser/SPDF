package com.snakeway.pdfviewer.model;

import android.graphics.Bitmap;

public class RenderedBitmap {
    private int canvasWidth;
    private int canvasHeight;
    private float pdfWidth;
    private float pdfHeight;
    private String base64;

    public RenderedBitmap(int canvasWidth, int canvasHeight, float pdfWidth, float pdfHeight, String base64) {
        super();
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.pdfWidth = pdfWidth;
        this.pdfHeight = pdfHeight;
        this.base64 = base64;
    }

    @Override
    public String toString() {
        return "RenderedBitmap{" +
                "canvasWidth=" + canvasWidth +
                ", canvasHeight=" + canvasHeight +
                ", pdfWidth=" + pdfWidth +
                ", pdfHeight=" + pdfHeight +
                ", base64='" + base64 + '\'' +
                '}';
    }
}