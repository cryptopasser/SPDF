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
import android.util.Log;
import android.util.SparseArray;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.annotation.MarkAnnotation;
import com.snakeway.pdfviewer.annotation.PenAnnotation;
import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.pdfviewer.annotation.pen.Pen;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/5 15:02
 */
final class AnnotationDrawManager {
    /**
     * 基础画笔线框系数
     */
    private static final int BASE_PEN_WIDTH_COEFFICIENT = 72;//SIZE = DisplayMetrics.DENSITY_MEDIUM;//160对应dpi的1倍倍率

    private final HashMap<Integer,String> drawedPageCache = new HashMap<Integer,String>();

    private PDFView pdfView;
    private AnnotationManager annotationManager;
    /**
     * 绘制中注释的画布
     */
    private Bitmap drawingBitmap;

    /**
     * 绘制中已抬手部分画笔的画布
     */
    private Bitmap haveDrawingPenBitmap;
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

    public void draw(Canvas canvas, int page) {
        Long textPagesPtr = pdfView.pdfFile.pdfDocument.getTextPagesPtr(page);//因为刷新前后几页可以自定义,比如前后2页可能超过viewpage渲染,如果当前页还没有渲染则跳过批注绘制
        if (textPagesPtr == null) {
            return;
        }
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
            float maxWidth = pdfView.pdfFile.getMaxPageWidth(pdfView.getCurrentPage());
            localTranslationX = (maxWidth - size.getWidth()) * zoom / 2;
        } else {
            localTranslationX = pdfView.pdfFile.getPageOffset(page, zoom);
            float maxHeight = pdfView.pdfFile.getMaxPageHeight(pdfView.getCurrentPage());
            localTranslationY = (maxHeight - size.getHeight()) * zoom / 2;
        }
        canvas.translate(localTranslationX, localTranslationY);
        //画 注释
        drawAnnotation(canvas, page, rect, rectF, scale);
        //画 区域选择
        drawAreaAnnotation(canvas, page, rect, rectF, scale);
        //画 搜索区域
        drawSearchAreaAnnotation(canvas, page, rect, rectF, scale);

        //画 正在绘制未保存画笔
        //drawDrawingPenAnnotation(canvas, page, rect, rectF, scale);
        //画 橡皮擦
        drawEraser(canvas, page, rect, rectF, scale);

        // 移动到原来的位置
        canvas.translate(-localTranslationX, -localTranslationY);
    }

    public void drawPenDrawing(Canvas canvas, int page,boolean isDrawing) {
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
            float maxWidth = pdfView.pdfFile.getMaxPageWidth(pdfView.getCurrentPage());
            localTranslationX = (maxWidth - size.getWidth()) * zoom / 2;
        } else {
            localTranslationX = pdfView.pdfFile.getPageOffset(page, zoom);
            float maxHeight = pdfView.pdfFile.getMaxPageHeight(pdfView.getCurrentPage());
            localTranslationY = (maxHeight - size.getHeight()) * zoom / 2;
        }
        canvas.translate(localTranslationX, localTranslationY);
        //画 正在绘制未保存画笔
        drawPenDrawingAnnotation(canvas, page, rect, rectF, scale,isDrawing);
        canvas.translate(-localTranslationX, -localTranslationY);
    }


    public boolean drawAnnotation(Canvas canvas,int targetWidth, int page) {
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        float scale = (float)pdfSize.getWidth() /targetWidth;
        int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
        basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
        List<BaseAnnotation> baseAnnotations=annotationManager.annotations.get(page);
        if(baseAnnotations==null||baseAnnotations.size()==0){
            return false;
        }
        for (BaseAnnotation annotation : baseAnnotations) {
            annotation.singleDraw(canvas, scale, basePenWidth, pdfView);
        }
        return true;
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
        boolean emptyDrawingMarkAnnotation = annotationManager.drawingMarkAnnotation == null ||
                annotationManager.drawingMarkAnnotation.page != page ||
                annotationManager.drawingMarkAnnotation.data == null;
        //如果不需要绘制，跳过绘制
        boolean needSkip = emptyAnnotation && emptyDrawingMarkAnnotation;
        if (needSkip) {
            return;
        }
        if (!emptyAnnotation) {
            drawDrawedAnnotation(canvas, page, pageSize, drawRegion, scale, annotations);
        }
        if (!emptyDrawingMarkAnnotation) {
            drawDrawingMarkAnnotation(canvas, page, pageSize, drawRegion, scale, annotationManager.drawingMarkAnnotation);
        }
    }

    private void checkAnnotationInit(List<BaseAnnotation> annotations) {
        if (annotations == null) {
            return;
        }
        for (BaseAnnotation annotation : annotations) {
            if (annotation.needInit && annotation instanceof MarkAnnotation) {
                annotationManager.initAnnotationData((MarkAnnotation) annotation);
            }
        }
    }


    /**
     * 正在绘制未保存画笔内容
     **/
    private void drawPenDrawingAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale,boolean isHaveDrawing) {
        if(isHaveDrawing){
            if (annotationManager.haveDrawingPenAnnotations.size() == 0) {
                return;
            }
            if (haveDrawingPenBitmap == null) {
                haveDrawingPenBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
            } else {
                if (haveDrawingPenBitmap.getWidth() != pageSize.width() || haveDrawingPenBitmap.getHeight() != pageSize.height()) {
                    haveDrawingPenBitmap.recycle();
                    haveDrawingPenBitmap = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
                }
            }
            Canvas penCanvas = new Canvas(haveDrawingPenBitmap);
            Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
            int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
            basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;

            for (PenAnnotation penAnnotation : annotationManager.haveDrawingPenAnnotations) {
                if (!penAnnotation.drawed) {
                    penAnnotation.draw(penCanvas, scale, basePenWidth, pdfView);
                }
            }
            canvas.drawBitmap(haveDrawingPenBitmap, pageSize, drawRegion, paint);
        }else{
            if (annotationManager.drawingPenAnnotation == null) {
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
            if(!pdfView.isDrawingPenOptimizeEnabled()){
                penCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//使用drawWithOptimize时候不要清空旧画布数据,通过pen的reset方法执行清理
            }
            Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
            int basePenWidth = Math.min(pdfSize.getHeight(), pdfSize.getWidth());
            basePenWidth /= BASE_PEN_WIDTH_COEFFICIENT;
            if (annotationManager.drawingPenAnnotation != null) {
                if(pdfView.isDrawingPenOptimizeEnabled()){
                    annotationManager.drawingPenAnnotation.drawWithOptimize(penCanvas, scale, basePenWidth, pdfView);
                }else{
                    annotationManager.drawingPenAnnotation.draw(penCanvas, scale, basePenWidth, pdfView);
                }
            }
            canvas.drawBitmap(drawingPenBitmap, pageSize, drawRegion, paint);
        }
    }
    /**
     * 绘制完成编辑的注释
     */
    private void drawDrawedAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale, List<BaseAnnotation> annotations) {
        Bitmap bm =getAnnotationCacheBitmap(page);
        if (bm == null) {
            if(drawedPageCache.get(page)!=null){//lrucache缓存的图片可能被回收了,导致原本缓存不存在了,这时候需要重新绘制
                setRecycleAnnotationPageUnDraw(page);
                drawedPageCache.remove(page);
            }
            bm = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
        } else {
            if (bm.getWidth() != pageSize.width() || bm.getHeight() != pageSize.height()) {
                bm.recycle();
                bm = Bitmap.createBitmap(pageSize.width(), pageSize.height(), Bitmap.Config.ARGB_8888);
                setRecycleAnnotationPageUnDraw(page);
            }
        }
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
        String cacheBitmapKey=putAnnotationCacheBitmap(page, bm);
        drawedPageCache.put(page, cacheBitmapKey);
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
    private void drawDrawingMarkAnnotation(Canvas canvas, int page, Rect pageSize, RectF drawRegion, float scale, BaseAnnotation drawingAnnotation) {
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

    /**
     * 不需要绘制的bitmap给予释放
     *
     * @param pages 不用回收的页
     */
    public  void recycle(List<Integer> pages) {
        for (Integer page : pages) {
            Bitmap bm = getAnnotationCacheBitmap(page);
            if (bm != null) {
                recycleAndSetUnDrawAnnotationCacheBitmap(page);
            }
        }
    }

    public  void recycleAndSetUnDrawAnnotationCacheBitmap(int page){
        clearAnnotationCacheBitmap(page);
        setRecycleAnnotationPageUnDraw(page);
        drawedPageCache.remove(page);
    }


    public Bitmap getDrawingBitmap() {
        return drawingBitmap;
    }

    public Bitmap getHaveDrawingPenBitmap() {
        return haveDrawingPenBitmap;
    }

    public Bitmap getDrawingPenBitmap() {
        return drawingPenBitmap;
    }

    public Bitmap getAreaBitmap() {
        return areaBitmap;
    }

    public Bitmap getSearchAreaBitmap() {
        return searchAreaBitmap;
    }

    public List<Integer> getDrawedPageCachePages() {
        return Arrays.asList(drawedPageCache.keySet().toArray(new Integer[drawedPageCache.size()]));
    }

    public String putAnnotationCacheBitmap(int page,Bitmap bitmap) {
        String key=CacheManager.ANNOTATION_CACHE_TAG+page;
        pdfView.cacheManager.getBitmapMemoryCacheHelper().putBitmap(key,bitmap);
        return key;
    }
    public Bitmap getAnnotationCacheBitmap(int page) {
        return  pdfView.cacheManager.getBitmapMemoryCacheHelper().getBitmap(CacheManager.ANNOTATION_CACHE_TAG+page);
    }
    public void clearAnnotationCacheBitmap(int page) {
        pdfView.cacheManager.getBitmapMemoryCacheHelper().clearMemory(CacheManager.ANNOTATION_CACHE_TAG+page);
    }

}
