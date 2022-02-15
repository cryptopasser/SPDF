package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class AreaPen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    private int cursorColor;

    private int targetViewSize = 60;

    AreaPen(int color) {
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
        int size = datas.size();
        if (size >= 1) {//绘制区域的起始和结束游标
            RectF startRect = datas.get(0);
            RectF endRect = size == 1 ? datas.get(0) : datas.get(size - 1);
            drawTargetView(canvas, startRect, true);
            drawTargetView(canvas, endRect, false);
        }
    }

    private void drawTargetView(Canvas canvas, RectF rect, boolean isStart) {
        if (rect == null) {
            return;
        }
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(cursorColor == 0 ? color : cursorColor);
        paint.setAntiAlias(true);
        paint.setDither(true);
        canvas.save();
        Path path = new Path();
        if (isStart) {
            float height = rect.bottom - rect.top;
            canvas.translate(rect.left, rect.top);
            path.addCircle(-targetViewSize / 2, targetViewSize / 2 + height, targetViewSize / 2, Path.Direction.CCW);
            RectF rectF = new RectF(-targetViewSize / 2, height, 0, height + targetViewSize / 2);
            path.addRect(rectF, Path.Direction.CCW);
        } else {
            canvas.translate(rect.right, rect.bottom);
            path.addCircle(targetViewSize / 2, targetViewSize / 2, targetViewSize / 2, Path.Direction.CCW);
            RectF rectF = new RectF(0, 0, targetViewSize / 2, targetViewSize / 2);
            path.addRect(rectF, Path.Direction.CCW);
        }
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override
    public PenType getPenType() {
        return PenType.AREA;
    }

    public int getTargetViewSize() {
        return targetViewSize;
    }

    public void setTargetViewSize(int targetViewSize) {
        this.targetViewSize = targetViewSize;
    }

    public int getCursorColor() {
        return cursorColor;
    }

    public void setCursorColor(int cursorColor) {
        this.cursorColor = cursorColor;
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

    public RectF getStartRectF(List<RectF> list, PDFView pdfView, int page, float scale) {
        List<RectF> datas = getMergeLineRect(list, pdfView, page, scale);
        int size = datas.size();
        if (size >= 1) {
            RectF startRect = datas.get(0);
            return startRect;
        }
        return null;
    }

    public RectF getEndRectF(List<RectF> list, PDFView pdfView, int page, float scale) {
        List<RectF> datas = getMergeLineRect(list, pdfView, page, scale);
        int size = datas.size();
        if (size >= 1) {
            RectF endRect = size == 1 ? datas.get(0) : datas.get(size - 1);
            return endRect;
        }
        return null;
    }
}
