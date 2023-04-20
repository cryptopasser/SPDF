package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class DeleteLinePen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    DeleteLinePen(int color) {
        this.color = color;
        init();
    }

    @Override
    public void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public void draw(List<RectF> list, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        if (list == null || list.size() == 0) {
            return;
        }
        List<RectF> datas = mergeRect(list, pdfView, page);
        for (RectF f : datas) {
            f.left /= scale;
            f.top /= scale;
            f.right /= scale;
            f.bottom /= scale;
            float y = (f.top + f.bottom) / 2;
            canvas.drawLine(f.left, y, f.right, y, paint);
        }
    }

    @Override
    public void drawWithOptimize(List<RectF> data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        this.draw(data,canvas,scale,basePenWidth,pdfView,page);
    }

    public void reset() {
    }


    @Override
    public PenType getPenType() {
        return PenType.DELETELINE;
    }

}
