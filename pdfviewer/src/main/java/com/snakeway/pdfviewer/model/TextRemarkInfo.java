package com.snakeway.pdfviewer.model;

import com.snakeway.pdflibrary.util.SizeF;

/**
 * @author snakeway
 */
public class TextRemarkInfo {
    private String key;
    private String data;
    private int x;
    private int y;
    private float zoom;
    private int page;
    private SizeF leftTopPdfSize;
    private SizeF rightBottomPdfSize;
    private int width;
    private int height;
    private float scale;

    public TextRemarkInfo() {
    }

    public TextRemarkInfo(String key, String data, int x, int y, float zoom, int page) {
        this.key = key;
        this.data = data;
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.page = page;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public SizeF getLeftTopPdfSize() {
        return leftTopPdfSize;
    }

    public void setLeftTopPdfSize(SizeF leftTopPdfSize) {
        this.leftTopPdfSize = leftTopPdfSize;
    }

    public SizeF getRightBottomPdfSize() {
        return rightBottomPdfSize;
    }

    public void setRightBottomPdfSize(SizeF rightBottomPdfSize) {
        this.rightBottomPdfSize = rightBottomPdfSize;
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

}
