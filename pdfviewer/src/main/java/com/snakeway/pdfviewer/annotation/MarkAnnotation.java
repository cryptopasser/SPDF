package com.snakeway.pdfviewer.annotation;

import android.graphics.RectF;

import androidx.annotation.Nullable;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdfviewer.annotation.base.AnnotationType;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.pdfviewer.annotation.pen.Pen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author snakeway
 */
public final class MarkAnnotation extends BaseAnnotation<List<RectF>, Pen.MarkPen> {
    public MarkAreaType markAreaType;
    public int startIndex;
    public int endIndex;

    public MarkAnnotation(int page, Size pageSize,float dpi, Pen.MarkPen pen, @Nullable MarkAreaType markAreaType, int startIndex, int endIndex, RectF rectF) {
        super(AnnotationType.MARK, page, pageSize,dpi, pen);
        data = new ArrayList<>();
        this.markAreaType = markAreaType;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.data = Arrays.asList(rectF);
    }

    public void update(int endIndex, List<RectF> rects) {
        this.endIndex = endIndex;
        this.data = new ArrayList<RectF>(rects);
    }

    public void updateAll(int startIndex, int endIndex, List<RectF> rects) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.data = new ArrayList<RectF>(rects);
    }

    public void updateRects(List<RectF> rects) {
        this.data = new ArrayList<RectF>(rects);
    }
}
