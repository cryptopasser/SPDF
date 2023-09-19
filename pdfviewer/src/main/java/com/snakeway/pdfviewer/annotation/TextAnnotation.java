package com.snakeway.pdfviewer.annotation;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.base.AnnotationType;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.pen.Pen;
import com.snakeway.pdfviewer.model.TextRemarkInfo;

/**
 * @author snakeway
 */
public final class TextAnnotation extends BaseAnnotation<TextRemarkInfo, Pen.TextPen> {
    private transient RectF areaRect;
    private transient boolean needHidden;

    public TextAnnotation(int page, Size pageSize,float dpi, Pen.TextPen pen) {
        super(AnnotationType.TEXT, page, pageSize,dpi, pen);
    }

    @Override
    public void draw(Canvas canvas, float scale, int basePenWidth, PDFView pdfView) {
        super.draw(canvas, scale, basePenWidth, pdfView);
//        drawed=false;
    }

    public RectF getAreaRect() {
        return areaRect;
    }

    public void setAreaRect(RectF areaRect) {
        this.areaRect = areaRect;
    }

    public boolean isNeedHidden() {
        return needHidden;
    }

    public void setNeedHidden(boolean needHidden) {
        this.needHidden = needHidden;
    }

    public RectF getPdfAreaRect(PDFView pdfView) {
        if (pdfView == null) {
            return new RectF(0, 0, 0, 0);
        }
        SizeF leftTopPdfSize = data.getLeftTopPdfSize();
        SizeF rightBottomPdfSize = data.getRightBottomPdfSize();

        Point leftTopPoint = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, leftTopPdfSize.getWidth(), leftTopPdfSize.getHeight());
        Point rightBottomPoint = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, rightBottomPdfSize.getWidth(), rightBottomPdfSize.getHeight());

        RectF resultRectF = new RectF(leftTopPoint.x, leftTopPoint.y, rightBottomPoint.x, rightBottomPoint.y);
        return resultRectF;
    }

}
