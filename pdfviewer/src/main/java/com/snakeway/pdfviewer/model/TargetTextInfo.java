package com.snakeway.pdfviewer.model;

import com.snakeway.pdflibrary.util.Size;

/**
 * @author snakeway
 */
public class TargetTextInfo {
    private int page;
    private Size pageSize;
    private int textIndex;
    private long pagePtr;
    private boolean inPage;
    private float scale;

    public TargetTextInfo(int page, Size pageSize, int textIndex, long pagePtr, boolean inPage, float scale) {
        this.page = page;
        this.pageSize = pageSize;
        this.textIndex = textIndex;
        this.pagePtr = pagePtr;
        this.inPage = inPage;
        this.scale = scale;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Size getPageSize() {
        return pageSize;
    }

    public void setPageSize(Size pageSize) {
        this.pageSize = pageSize;
    }

    public int getTextIndex() {
        return textIndex;
    }

    public void setTextIndex(int textIndex) {
        this.textIndex = textIndex;
    }

    public long getPagePtr() {
        return pagePtr;
    }

    public void setPagePtr(long pagePtr) {
        this.pagePtr = pagePtr;
    }

    public boolean isInPage() {
        return inPage;
    }

    public void setInPage(boolean inPage) {
        this.inPage = inPage;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }
}
