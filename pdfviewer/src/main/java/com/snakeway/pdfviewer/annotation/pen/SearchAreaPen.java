package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class SearchAreaPen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    SearchAreaPen(int color) {
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
        List<RectF> datas = getMergeLineRect(list, pdfView, page, scale);
        for (RectF f : datas) {
            canvas.drawRect(f, paint);
        }
    }

    @Override
    public PenType getPenType() {
        return PenType.SEARCHAREA;
    }

    public List<RectF> getMergeLineRect(List<RectF> list, PDFView pdfView, int page, float scale) {
        List<RectF> lineRects = mergeRect(list, pdfView, page);
        for (RectF f : lineRects) {
            f.left /= scale;
            f.top /= scale;
            f.right /= scale;
            f.bottom /= scale;
        }
        return lineRects;
    }

}
