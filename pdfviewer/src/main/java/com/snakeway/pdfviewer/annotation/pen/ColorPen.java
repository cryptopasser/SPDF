package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class ColorPen implements Pen.WritePen {

    private transient Paint paint;

    private int color;

    private float penWidthScale;

    public ColorPen(int color, float penWidthScale) {
        this.color = color;
        this.penWidthScale = penWidthScale;
        init();
    }

    @Override
    public void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public void draw(List<SizeF> points, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        paint.setStrokeWidth(basePenWidth * penWidthScale / scale);
        paint.setPathEffect(new CornerPathEffect(paint.getStrokeWidth()));
        Path path = new Path();
        float x = points.get(0).getWidth();
        float y = points.get(0).getHeight();
        Point point = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, x, y);
        path.moveTo(point.x / scale, point.y / scale);
        for (int i = 1; i < points.size(); i++) {
            x = points.get(i).getWidth();
            y = points.get(i).getHeight();
            point = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, x, y);
            path.lineTo(point.x / scale, point.y / scale);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public PenType getPenType() {
        return PenType.COLORPEN;
    }

}
