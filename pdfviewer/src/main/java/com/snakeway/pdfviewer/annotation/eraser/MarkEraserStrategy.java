package com.snakeway.pdfviewer.annotation.eraser;

import android.graphics.Rect;
import android.graphics.RectF;

import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.MarkAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

/**
 * @author snakeway
 */
public class MarkEraserStrategy implements EraserStrategy {

    private static final RectF TEMP = new RectF();

    @Override
    public boolean erase(PDFView view, BaseAnnotation annotation, Rect rect) {
        if (annotation instanceof MarkAnnotation) {
            MarkAnnotation a = (MarkAnnotation) annotation;
            for (RectF rectItem : a.data) {
                RectF f = CoordinateUtils.toPdfPointCoordinateDesc(view, a.page, rectItem);
                TEMP.set(rect);
                if (RectF.intersects(f, TEMP)) {
                    return true;
                }
            }
        }
        return false;
    }
}
