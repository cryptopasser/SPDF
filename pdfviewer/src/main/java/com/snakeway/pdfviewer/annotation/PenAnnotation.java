package com.snakeway.pdfviewer.annotation;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.base.AnnotationType;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.pen.Pen;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snakeway
 */
public final class PenAnnotation extends BaseAnnotation<List<SizeF>, Pen.WritePen> {
    private transient RectF areaRect;
    private transient Rect cancelAreaRect;

    public PenAnnotation(int page, Size pageSize,float dpi, Pen.WritePen pen) {
        super(AnnotationType.PEN, page, pageSize,dpi, pen);
        data = new ArrayList<>();
    }


    public RectF getAreaRect() {
        return areaRect;
    }

    public void setAreaRect(RectF areaRect) {
        this.areaRect = areaRect;
        if (areaRect == null) {
            this.cancelAreaRect = null;
        }
    }

    public Rect getCancelAreaRect() {
        return cancelAreaRect;
    }

    public void setCancelAreaRect(Rect cancelAreaRect) {
        this.cancelAreaRect = cancelAreaRect;
    }

    public RectF getPdfAreaRect(PDFView pdfView) {
        if (pdfView == null) {
            return new RectF(0, 0, 0, 0);
        }
        float minX = 0;
        float maxX = 0;
        float minY = 0;
        float maxY = 0;
        for (int i = 0; i < data.size(); i++) {
            SizeF sizeF = data.get(i);
            Point point = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, sizeF.getWidth(), sizeF.getHeight());
            if (i == 0) {
                minX = point.x;
                maxX = point.x;
                minY = point.y;
                maxY = point.y;
            } else {
                if (point.x < minX) {
                    minX = point.x;
                } else if (point.x > maxX) {
                    maxX = point.x;
                }
                if (point.y < minY) {
                    minY = point.y;
                } else if (point.y > maxY) {
                    maxY = point.y;
                }
            }
        }
        return new RectF(minX, minY, maxX, maxY);
    }


    public RectF getScaleAreaRect(PDFView pdfView, RectF areaRect, float scale) {
        if (pdfView == null) {
            return new RectF(0, 0, 0, 0);
        }
        RectF resultRectF = new RectF(areaRect.left, areaRect.top, areaRect.right, areaRect.bottom);
        resultRectF.left /= scale;
        resultRectF.top /= scale;
        resultRectF.right /= scale;
        resultRectF.bottom /= scale;
        float zoom = pdfView.getZoom();
        resultRectF.left *= zoom;
        resultRectF.top *= zoom;
        resultRectF.right *= zoom;
        resultRectF.bottom *= zoom;
        return resultRectF;
    }

}
