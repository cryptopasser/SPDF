package com.snakeway.pdfviewer.model;

import android.graphics.RectF;

import java.util.List;

/**
 * @author snakeway
 */
public class SearchTextInfo {
    private int start;
    private int end;
    private List<RectF> data;
    private int page;

    public SearchTextInfo() {
    }

    public SearchTextInfo(int start, int end, List<RectF> data) {
        this.start = start;
        this.end = end;
        this.data = data;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public List<RectF> getData() {
        return data;
    }

    public void setData(List<RectF> data) {
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    @Override
    public String toString() {
        return "SearchTextInfo{" +
                "start=" + start +
                ", end=" + end +
                ", data=" + data +
                ", page=" + page +
                '}';
    }
}
