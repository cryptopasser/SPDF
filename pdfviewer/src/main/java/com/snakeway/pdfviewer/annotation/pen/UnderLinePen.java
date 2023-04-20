package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class UnderLinePen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    UnderLinePen(int color) {
        this.color = color;
        init();
    }

    @Override
    public void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setDither(true);
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
            canvas.drawLine(f.left, f.bottom + 2, f.right, f.bottom + 2, paint);
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
        return PenType.UNDERLINE;
    }

}
