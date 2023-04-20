package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.model.TextRemarkInfo;

import java.util.List;

/**
 * @author snakeway
 */
public class TextPen implements Pen.TextPen {
    private transient Paint paint;
    private int color;
    private float fontSize;

    public TextPen(int color,float fontSize) {
        this.color = color;
        this.fontSize=fontSize;
        init();
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    @Override
    public void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    @Override
    public void draw(TextRemarkInfo data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        Bitmap bitmap = pdfView.getTextRemarkBitmapCache(data.getKey(),data.getData(), color, data.getZoom(),true);
        if (bitmap == null) {
            return;
        }
        SizeF leftTopPdfSize = data.getLeftTopPdfSize();
        SizeF rightBottomPdfSize = data.getRightBottomPdfSize();

        Point leftTopPoint = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, leftTopPdfSize.getWidth(), leftTopPdfSize.getHeight());
        Point rightBottomPoint = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, rightBottomPdfSize.getWidth(), rightBottomPdfSize.getHeight());

        RectF resultRectF = new RectF(leftTopPoint.x, leftTopPoint.y, rightBottomPoint.x, rightBottomPoint.y);
        resultRectF.left /= scale;
        resultRectF.top /= scale;
        resultRectF.right /= scale;
        resultRectF.bottom /= scale;
        Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, src, resultRectF, paint);
    }

    @Override
    public void drawWithOptimize(TextRemarkInfo data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        this.draw(data,canvas,scale,basePenWidth,pdfView,page);
    }

    public void reset() {
    }


    @Override
    public PenType getPenType() {
        return PenType.TEXTPEN;
    }

}
