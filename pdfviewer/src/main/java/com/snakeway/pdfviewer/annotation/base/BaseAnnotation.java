package com.snakeway.pdfviewer.annotation.base;

import android.graphics.Canvas;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.util.ObjectUtil;

import java.util.UUID;

/**
 * @author snakeway
 */
public abstract class BaseAnnotation<T, Pen extends com.snakeway.pdfviewer.annotation.pen.Pen<T>> {
    public transient boolean drawed;
    public transient boolean needInit = false;

    public String id;
    public int page;
    public Pen pen;
    public T data;
    public AnnotationType annotationType;
    public Size pageSize;

    public BaseAnnotation(@NonNull AnnotationType annotationType, @IntRange(from = 0) int page, Size pageSize, @NonNull Pen pen) {
        this.id = UUID.randomUUID().toString();
        this.annotationType = annotationType;
        this.page = page;
        this.pen = pen;
        this.pageSize = pageSize;
    }

    public void singleDraw(Canvas canvas, float scale, int basePenWidth, PDFView pdfView) {
        pen.draw(data, canvas, scale, basePenWidth, pdfView, page);
    }

    public void draw(Canvas canvas, float scale, int basePenWidth, PDFView pdfView) {
        pen.draw(data, canvas, scale, basePenWidth, pdfView, page);
        drawed = true;
    }

    public void drawWithOptimize(Canvas canvas, float scale, int basePenWidth, PDFView pdfView) {
        pen.drawWithOptimize(data, canvas, scale, basePenWidth, pdfView, page);
        drawed = true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        BaseAnnotation baseAnnotation = (BaseAnnotation) obj;
        return ObjectUtil.isObjectEquals(id, baseAnnotation.id);
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return 0;
        } else {
            return id.hashCode();
        }
    }

}
