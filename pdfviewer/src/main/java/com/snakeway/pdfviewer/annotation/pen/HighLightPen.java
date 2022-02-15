package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class HighLightPen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    HighLightPen(int color) {
        this.color = color;
        init();
    }


    @Override
    public void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
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
            canvas.drawRect(f, paint);
        }
    }

    @Override
    public PenType getPenType() {
        return PenType.HIGHLIGHTPEN;
    }

}
