package com.snakeway.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.annotation.MarkAnnotation;
import com.snakeway.pdfviewer.annotation.PenAnnotation;
import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/5 15:02
 */
final class AnnotationDrawManager {
    /**
     * 基础画笔线框系数
     */
    private static final int BASE_PEN_WIDTH_COEFFICIENT = 100;

    private PDFView pdfView;
    private AnnotationManager annotationManager;
    /**
     * bitmap缓存
     */
    SparseArray<Bitmap> cache = new SparseArray<>();
    /**
     * 绘制中注释的画布
     */
    private Bitmap drawingBitmap;
    /**
     * 绘制中画笔的画布
     */
    private Bitmap drawingPenBitmap;
    /**
     * 区域的画布
     */
    private Bitmap areaBitmap;
    /**
     * 搜索区域的画布
     */
    private Bitmap searchAreaBitmap;
    /**
     * 画笔，绘制bitmap到大画布上
     */
    private Paint paint;

    AnnotationDrawManager(PDFView pdfView, AnnotationManager annotationManager) {
        this.pdfView = pdfView;
        this.annotationManager = annotationManager;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);
    }

    /**
     * 绘制
     */
    public void draw(Canvas canvas, int page) {
        //绘制需要的一些参数
        SizeF size = pdfView.pdfFile.getPageSize(page);
        float zoom = pdfView.getZoom();
        Rect rect = new Rect(0, 0, (int) size.getWidth(), (int) size.getHeight());
        RectF rectF = new RectF(0, 0, size.getWidth() * zoom, size.getHeight() * zoom);
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        float scale = pdfSize.getWidth() / size.getWidth();
        //移动到正确位置
        float localTranslationX;
        float localTranslationY;
        if (pdfView.isSwipeVertical()) {
            localTranslationY = pdfView.pdfFile.getPageOffset(page, zoom);
            float maxWidth = pdfView.pdfFile.getMaxPageWidth();
            localTranslationX = (maxWidth - size.getWidth()) * zoom / 2;
        } else {
            localTranslationX = pdfView.pdfFile.getPageOffset(page, zoom);
            float maxHeight = pdfView.pdfFile.getMaxPageHeight();
            localTranslationY = (maxHeight - size.getHeight()) * zoom / 2;
        }
        canvas.translate(localTranslationX, localTranslationY);
        //画 注释
        drawAnnotation(canvas, page, rect, rectF, scale);
        //画 画笔区域
        drawPenAnnotation(canvas, page, rect, rectF, scale);
        //画 橡皮擦
        drawEraser(canvas, page, rect, rectF, scale);
        //画 区域选择
        drawAreaAnnotation(canvas, page, rect, rectF, scale);
        //画 搜索区域
        drawSearchAreaAnnotation(canvas, page, rect, rectF, scale);
        // 移动到原来的位置
        canvas.translate(-localTranslationX, -localTranslationY);
    }

    /**
     * 画橡皮擦
     */
    private void drawEraser(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale) {
        if (
                pdfView.getFunction() == PDFView.Function.ERASER &&
                        annotationManager.eraser != null &&
                        annotationManager.eraserPage == page
        ) {
            if (drawingBitmap == null) {
                drawingBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            } else {
                if (drawingBitmap.getWidth() != pageSize.width() || drawingBitmap.getHeight() != pageSize.height()) {
                    drawingBitmap.recycle();
                    drawingBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
                }
            }
            Canvas drawingCanvas = new Canvas(drawingBitmap);
            drawingCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            annotationManager.eraser.draw(drawingCanvas, scale, pdfView.getZoom());
            canvas.drawBitmap(drawingBitmap, pageSize, drawRegion, paint);
        }
    }

    /**
     * 画注释
     *
     * @param canvas     画布
     * @param page       需要绘画的页码
     * @param pageSize   当前页码在display坐标上的大小
     * @param drawRegion bitmap画在canvas上的区域
     * @param scale      解析出来的pdf页码大小 于 display坐标dpf页码大小的比
     */
    private void drawAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale) {
        List<BaseAnnotation> annotations = annotationManager.annotations.get(page);
        checkAnnotationInit(annotations);
        //没有需要绘制的注释
        boolean emptyAnnotation = annotations == null || annotations.size() == 0;
        //没有需要绘制的正在绘制的注释
        boolean emptyDrawingAnnotaion = annotationManager.drawingAnnotation == null ||
                annotationManager.drawingAnnotation.page != page ||
                annotationManager.drawingAnnotation.data == null;
        //如果不需要绘制，跳过绘制
        boolean needSkip = emptyAnnotation && emptyDrawingAnnotaion;
        if (needSkip) {
            return;
        }
        if (!emptyAnnotation) {
            drawDrawedAnnotation(canvas, page, pageSize, drawRegion, scale, annotations);
        }
        if (!emptyDrawingAnnotaion) {
            drawDrawingAnnotation(canvas, page, pageSize, drawRegion, scale, annotationManager.drawingAnnotation);
        }
    }

    private void checkAnnotationInit(List<BaseAnnotation> annotations) {
        if (annotations == null) {
            return;
        }
        for (BaseAnnotation annotation : annotations) {
            if (annotation.needInit && annotation instanceof MarkAnnotation) {
                annotationManager.loadData((MarkAnnotation) annotation);
            }
        }
    }


    /**
     * 画 画笔区域
     **/
    private void drawPenAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale) {
        if (annotationManager.drawingPenAnnotations.size() == 0 && annotationManager.drawingPenAnnotation == null) {
            return;
        }
        if (drawingPenBitmap == null) {
            drawingPenBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (drawingPenBitmap.getWidth() != pageSize.width() || drawingPenBitmap.getHeight() != pageSize.height()) {
                drawingPenBitmap.recycle();
                drawingPenBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            }
        }
        Canvas penCanvas = new Canvas(drawingPenBitmap);
        penCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;

        for (PenAnnotation penAnnotation : annotationManager.drawingPenAnnotations) {
            penAnnotation.draw(penCanvas, scale, basePenWidth, pdfView);
            canvas.drawBitmap(drawingPenBitmap, pageSize, drawRegion, paint);
        }
        if (annotationManager.drawingPenAnnotation != null) {
            annotationManager.drawingPenAnnotation.draw(penCanvas, scale, basePenWidth, pdfView);
            canvas.drawBitmap(drawingPenBitmap, pageSize, drawRegion, paint);
        }
    }

    /**
     * 绘制完成编辑的注释
     */
    private void drawDrawedAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale, List<BaseAnnotation> annotations) {
        Bitmap bm = cache.get(page);
        if (bm == null) {
            bm = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (bm.getWidth() != pageSize.width() || bm.getHeight() != pageSize.height()) {
                bm.recycle();
                bm = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
                setRecycleAnnotationPageUnDraw(page);
            }
        }
        cache.put(page, bm);
        //需要绘画页面pdf解析尺寸
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth()); //基础画笔线框 最小宽/高 除以 100
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
        Canvas pageCanvas = new Canvas(bm);
        if (annotations != null && annotations.size() > 0) {
            for (BaseAnnotation annotation : annotations) {
                if(annotation instanceof TextAnnotation){
                    TextAnnotation textAnnotation=(TextAnnotation)annotation;
                    if (!textAnnotation.isNeedHidden()){
                        if (!annotation.drawed) {
                            annotation.draw(pageCanvas, scale, basePenWidth, pdfView);
                        }
                    }
                }else {
                    if (!annotation.drawed) {
                        annotation.draw(pageCanvas, scale, basePenWidth, pdfView);
                    }
                }
            }
        }
        //绘制在大的画布上
        canvas.drawBitmap(bm, pageSize, drawRegion, paint);
        drawPenCancel(canvas, page, scale, annotations, pdfSize.getWidth(), pdfSize.getHeight());
    }

    private void drawPenCancel(Canvas canvas, int page, float scale, List<BaseAnnotation> annotations, int width, int height) {
        if (annotations == null || annotations.size() == 0) {
            return;
        }
        int halfWidth = (int) (width / scale / 2 * pdfView.getZoom());
        int halfHeight = (int) (height / scale / 2 * pdfView.getZoom());
        for (BaseAnnotation annotation : annotations) {
            if (annotation instanceof PenAnnotation) {
                PenAnnotation penAnnotation = (PenAnnotation) annotation;
                if (penAnnotation.getAreaRect() != null) {
                    RectF resultRectF = penAnnotation.getScaleAreaRect(pdfView, penAnnotation.getAreaRect(), scale);
                    Paint paintLine = new Paint();
                    paintLine.setStyle(Paint.Style.STROKE);
                    paintLine.setColor(Color.DKGRAY);
                    paintLine.setStrokeWidth(3);
                    Path path = new Path();
                    path.moveTo(resultRectF.left, resultRectF.top);
                    path.lineTo(resultRectF.right, resultRectF.top);
                    path.lineTo(resultRectF.right, resultRectF.bottom);
                    path.lineTo(resultRectF.left, resultRectF.bottom);
                    path.lineTo(resultRectF.left, resultRectF.top);
                    PathEffect effects = new DashPathEffect(new float[]{8, 8, 8, 8}, 1);
                    paintLine.setPathEffect(effects);
                    canvas.drawPath(path, paintLine);
                    if (pdfView.getCancelBitmap() != null) {
                        float halfCancelBitmapSize = pdfView.getCancelBitmapSize() / 2 * pdfView.getZoom();
                        int left = 0;
                        int top = 0;
                        int right = 0;
                        int bottom = 0;
                        float resultAreaCenterX = (resultRectF.left + resultRectF.right) / 2;
                        float resultAreaCenterY = (resultRectF.top + resultRectF.bottom) / 2;
                        if (resultAreaCenterX >= halfWidth && resultAreaCenterY >= halfHeight) {
                            left = (int) (resultRectF.left - halfCancelBitmapSize);
                            top = (int) (resultRectF.top - halfCancelBitmapSize);
                            right = (int) (resultRectF.left + halfCancelBitmapSize);
                            bottom = (int) (resultRectF.top + halfCancelBitmapSize);
                        } else if (resultAreaCenterX >= halfWidth && resultAreaCenterY <= halfHeight) {
                            left = (int) (resultRectF.left - halfCancelBitmapSize);
                            top = (int) (resultRectF.bottom - halfCancelBitmapSize);
                            right = (int) (resultRectF.left + halfCancelBitmapSize);
                            bottom = (int) (resultRectF.bottom + halfCancelBitmapSize);
                        } else if (resultAreaCenterX <= halfWidth && resultAreaCenterY <= halfHeight) {
                            left = (int) (resultRectF.right - halfCancelBitmapSize);
                            top = (int) (resultRectF.bottom - halfCancelBitmapSize);
                            right = (int) (resultRectF.right + halfCancelBitmapSize);
                            bottom = (int) (resultRectF.bottom + halfCancelBitmapSize);
                        } else {
                            left = (int) (resultRectF.right - halfCancelBitmapSize);
                            top = (int) (resultRectF.top - halfCancelBitmapSize);
                            right = (int) (resultRectF.right + halfCancelBitmapSize);
                            bottom = (int) (resultRectF.top + halfCancelBitmapSize);
                        }
                        Bitmap cancelBitmap = pdfView.getCancelBitmap();
                        Rect src = new Rect(0, 0, cancelBitmap.getWidth(), cancelBitmap.getHeight());
                        Rect dst = new Rect(left, top, right, bottom);
                        penAnnotation.setCancelAreaRect(dst);
                        canvas.drawBitmap(cancelBitmap, src, dst, paint);
                    }
                }
            }
        }
    }


    /**
     * 绘制正在编辑的注释
     */
    private void drawDrawingAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale, BaseAnnotation drawingAnnotation) {
        //如果bitmap 为空创建，如果bitmap的尺寸不是需要的bitmap重新创建
        if (drawingBitmap == null) {
            drawingBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (drawingBitmap.getWidth() != pageSize.width() || drawingBitmap.getHeight() != pageSize.height()) {
                drawingBitmap.recycle();
                drawingBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            }
        }
        Canvas drawingCanvas = new Canvas(drawingBitmap);
        drawingCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //基础画笔线框 最小宽/高 除以 100
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
        //绘制注释
        drawingAnnotation.draw(drawingCanvas, scale, basePenWidth, pdfView);
        drawingAnnotation.drawed = false;
        //绘制到大画布上
        canvas.drawBitmap(drawingBitmap, pageSize, drawRegion, paint);
    }

    /**
     * 绘制区域选择
     */
    private void drawAreaAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale) {
        boolean emptyAnnotation = annotationManager.areaMarkAnnotation == null ||
                annotationManager.areaMarkAnnotation.page != page ||
                annotationManager.areaMarkAnnotation.data == null;
        if (emptyAnnotation) {
            return;
        }
        if (areaBitmap == null) {
            areaBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (areaBitmap.getWidth() != pageSize.width() || areaBitmap.getHeight() != pageSize.height()) {
                areaBitmap.recycle();
                areaBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            }
        }
        Canvas areaCanvas = new Canvas(areaBitmap);
        //清空画布
        areaCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //基础画笔线框 最小宽/高 除以 100
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
        //绘制注释
        annotationManager.areaMarkAnnotation.draw(areaCanvas, scale, basePenWidth, pdfView);
//        annotationManager.areaMarkAnnotation.drawed = false;
        //绘制到大画布上

        canvas.drawBitmap(areaBitmap, pageSize, drawRegion, paint);
    }


    /**
     * 绘制搜索区域
     */
    private void drawSearchAreaAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale) {
        boolean emptyAnnotation = annotationManager.searchMarkAnnotation == null ||
                annotationManager.searchMarkAnnotation.page != page ||
                annotationManager.searchMarkAnnotation.data == null;
        if (emptyAnnotation) {
            return;
        }
        if (searchAreaBitmap == null) {
            searchAreaBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (searchAreaBitmap.getWidth() != pageSize.width() || searchAreaBitmap.getHeight() != pageSize.height()) {
                searchAreaBitmap.recycle();
                searchAreaBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            }
        }
        Canvas areaCanvas = new Canvas(searchAreaBitmap);
        areaCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
        annotationManager.searchMarkAnnotation.draw(areaCanvas, scale, basePenWidth, pdfView);
        canvas.drawBitmap(searchAreaBitmap, pageSize, drawRegion, paint);
    }


    /**
     * 不需要绘制的bitmap给予释放
     *
     * @param pages 不用回收的页
     */
    void recycle(List<Integer> pages) {
        List<Integer> recyclePage = new ArrayList<>();
        for (int i = 0; i < cache.size(); i++) {
            if (!pages.contains(cache.keyAt(i))) {
                recyclePage.add(cache.keyAt(i));
            }
        }
        for (Integer page : recyclePage) {
            Bitmap bm = cache.get(page);
            if (bm != null && !bm.isRecycled()) {
                bm.recycle();
            }
            cache.remove(page);
            setRecycleAnnotationPageUnDraw(page);
        }
    }

    /**
     * 设置被回收的页码的注释为未绘制状态
     */
    private void setRecycleAnnotationPageUnDraw(int page) {
        List<BaseAnnotation> list = annotationManager.annotations.get(page);
        if (list == null || list.size() == 0) {
            return;
        }
        for (BaseAnnotation annotation : list) {
            annotation.drawed = false;
        }
    }
}
