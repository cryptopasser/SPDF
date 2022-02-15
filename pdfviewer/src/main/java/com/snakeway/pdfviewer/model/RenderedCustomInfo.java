package com.snakeway.pdfviewer.model;

import android.graphics.Bitmap;

public class RenderedCustomInfo {
    private int page;
    private int width;
    private int height;
    private float scale;
    private float left;
    private float top;
    private float right;
    private float bottom;
    private Bitmap renderedBitmap;

    public RenderedCustomInfo(int page, int width, int height, float scale, float left, float top, float right, float bottom, Bitmap renderedBitmap) {
        super();
        this.page = page;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.renderedBitmap = renderedBitmap;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    public Bitmap getRenderedBitmap() {
        return renderedBitmap;
    }

    public void setRenderedBitmap(Bitmap renderedBitmap) {
        this.renderedBitmap = renderedBitmap;
    }

    @Override
    public String toString() {
        return "RenderingCustomInfo{" +
                "page=" + page +
                ", width=" + width +
                ", height=" + height +
                ", scale=" + scale +
                ", left=" + left +
                ", top=" + top +
                ", right=" + right +
                ", bottom=" + bottom +
                ", renderedBitmap=" + renderedBitmap +
                '}';
    }
}