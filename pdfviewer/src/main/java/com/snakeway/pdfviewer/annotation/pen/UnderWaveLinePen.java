package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;

import java.util.List;

/**
 * @author snakeway
 */
public class UnderWaveLinePen implements Pen.MarkPen {

    private transient Paint paint;

    private int color;

    private int waveMaxLineCount = 100;

    UnderWaveLinePen(int color) {
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
            drawWaveLine(f, canvas);
        }
    }

    private void drawWaveLine(RectF rectF, Canvas canvas) {
        int canvasWidth = canvas.getWidth();

        float lineWidth = rectF.right - rectF.left;
        int lineWidthCount = (int) Math.ceil(lineWidth / canvasWidth * waveMaxLineCount);

        float doubleWaveSize = lineWidth / lineWidthCount;
        float waveSize = doubleWaveSize / 2;
        float bottom = doubleWaveSize / 2;

        float startX = rectF.left;
        float startY = rectF.bottom;

        for (int i = 0; i < lineWidthCount; i++) {
            canvas.drawLine(startX + i * doubleWaveSize, startY + bottom, startX + i * doubleWaveSize + waveSize, startY, paint);
            canvas.drawLine(startX + i * doubleWaveSize + waveSize, startY, startX + i * doubleWaveSize + waveSize * 2, startY + bottom, paint);
        }
    }

    public int getWaveMaxLineCount() {
        return waveMaxLineCount;
    }

    public void setWaveMaxLineCount(int waveMaxLineCount) {
        this.waveMaxLineCount = waveMaxLineCount;
    }

    @Override
    public void drawWithOptimize(List<RectF> data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page) {
        this.draw(data,canvas,scale,basePenWidth,pdfView,page);
    }

    public void reset() {
    }


    @Override
    public PenType getPenType() {
        return PenType.UNDERWAVELINE;
    }

}
