package com.snakeway.pdfviewer.annotation.eraser;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

import java.util.List;

/**
 * @author snakeway
 */
public class RectEraser implements Eraser {
    //橡皮的宽度
    private int width;
    //橡皮的高度
    private int height;
    //中心点位置
    private int x = Integer.MIN_VALUE;
    private int y = Integer.MIN_VALUE;
    private Paint paint;
    private boolean zoomable;

    private EraserStrategy[] mEraserStrategyArray;

    /**
     * @param width    基于display 坐标的宽度
     * @param height   基于display 坐标的高
     * @param zoomable 是否缩放，true 橡皮擦大小随view缩放变小，false 固定大小
     */
    public RectEraser(int width, int height, boolean zoomable, EraserStrategy... eraserStrategies) {
        this.width = width;
        this.height = height;
        this.zoomable = zoomable;
        this.mEraserStrategyArray = eraserStrategies;
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GRAY);
    }

    @Override
    public void move(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw(Canvas canvas, float scale, float viewZoom) {
        float widthSpace = width / 2;
        float heightSpace = height / 2;
        if (zoomable) {
            widthSpace /= viewZoom;
            heightSpace /= viewZoom;
        }
        RectF rectF = new RectF(
                x / scale - widthSpace,
                y / scale - heightSpace,
                x / scale + widthSpace,
                y / scale + heightSpace
        );
        float shadowSize = Math.min(widthSpace, heightSpace);
        shadowSize /= 10;
        paint.setShadowLayer(shadowSize, shadowSize, shadowSize, Color.GRAY);
        canvas.drawRect(rectF, paint);
    }

    @Override
    public BaseAnnotation erase(List<BaseAnnotation> annotations, float scale, float viewZoom, PDFView pdfView) {
        if (annotations == null || annotations.size() == 0) {
            return null;
        }
        int widthSpace = (int) (width * scale / 2);
        int heightSpace = (int) (height * scale / 2);
        if (zoomable) {
            widthSpace /= viewZoom;
            heightSpace /= viewZoom;
        }
        Rect rect = new Rect(x - widthSpace, y - heightSpace, x + widthSpace, y + heightSpace);

        for (BaseAnnotation annotation : annotations) {
            for (EraserStrategy strategy : mEraserStrategyArray) {
                if (strategy.erase(pdfView, annotation, rect)) {
                    return annotation;
                }
            }
        }
        return null;
    }
}
