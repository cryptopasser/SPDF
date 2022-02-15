package com.snakeway.pdfviewer.annotation.eraser;

import android.graphics.Canvas;

import androidx.annotation.Nullable;

import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

import java.util.List;

/**
 * @author snakeway
 */
public interface Eraser {
    /**
     * 移动橡皮擦
     *
     * @param x 基于pdf坐标的x
     * @param y 基于pdf坐标的y
     */
    void move(int x, int y);

    /**
     * 绘制橡皮擦
     *
     * @param canvas   画布,大小为display坐标中pdf页面的大小
     * @param scale    pdf坐标页面大小于display坐标页面大小比例
     * @param viewZoom {{@link PDFView#getZoom()}}
     */
    void draw(Canvas canvas, float scale, float viewZoom);

    /**
     * 获取需要擦除的注释
     *
     * @param annotations 当前页的注释
     * @param scale       pdf坐标页面大小于display坐标页面大小比例
     * @param viewZoom    {{@link PDFView#getZoom()}}
     * @return 需要擦除的注释
     */
    BaseAnnotation erase(@Nullable List<BaseAnnotation> annotations, float scale, float viewZoom, PDFView pdfView);
}
