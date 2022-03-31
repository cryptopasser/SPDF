package com.snakeway.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;

import androidx.annotation.IntRange;
import androidx.annotation.Nullable;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.annotation.AnnotationListener;
import com.snakeway.pdfviewer.annotation.MarkAnnotation;
import com.snakeway.pdfviewer.annotation.PenAnnotation;
import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.pdfviewer.annotation.eraser.Eraser;
import com.snakeway.pdfviewer.annotation.pen.AreaPen;
import com.snakeway.pdfviewer.annotation.pen.Pen;
import com.snakeway.pdfviewer.annotation.pen.PenBuilder;
import com.snakeway.pdfviewer.annotation.pen.SearchAreaPen;
import com.snakeway.pdfviewer.annotation.pen.TextPen;
import com.snakeway.pdfviewer.listener.OnAreaTouchListener;
import com.snakeway.pdfviewer.model.SearchTextInfo;
import com.snakeway.pdfviewer.model.TargetTextInfo;
import com.snakeway.pdfviewer.model.TextRemarkInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/5 15:02
 */

final class AnnotationManager {
    private static final String TAG = AnnotationManager.class.getName();
    private final float TOUCH_TOLERANCE = 3;
    private PDFView pdfView;
    //注释缓存
    SparseArray<List<BaseAnnotation>> annotations = new SparseArray<>();

    List<PenAnnotation> drawingPenAnnotations = new ArrayList<>();

    //绘制中的注释
    BaseAnnotation drawingAnnotation;
    //画笔备注
    PenAnnotation drawingPenAnnotation;
    //区域选择提示
    MarkAnnotation areaMarkAnnotation;
    //搜索文字提示
    MarkAnnotation searchMarkAnnotation;
    //当前绘制的画笔
    private Pen pen;
    //区域选择画笔
    private AreaPen areaPen;
    //搜索区域选中画笔
    private SearchAreaPen searchAreaPen;
    //区域选择画笔
    private TextPen textPen;
    //区域选择-画笔
    private HashMap<MarkAreaType, Pen.MarkPen> areaMarkPens = new HashMap<>();
    //橡皮擦
    Eraser eraser;
    //橡皮擦按下时所在的页码,-1表示未按下
    int eraserPage = -1;

    AnnotationListener annotationListener;

    TargetTextInfo downTargetTextInfo;

    TargetTextInfo currentTargetTextInfo;

    final List<RectF> areaRects = new ArrayList<>();

    boolean areaSelect;

    float areaDownX;

    float areaDownY;

    RectF startTargetRect;
    RectF endTargetRect;

    float penTouchX=0;
    float penTouchY=0;

    AnnotationManager(PDFView pdfView) {
        this.pdfView = pdfView;
    }

    public void setAnnotationListener(AnnotationListener annotationListener) {
        this.annotationListener = annotationListener;
    }

    void setPen(Pen pen) {
        this.pen = pen;
    }

    public void setTextPen(TextPen textPen) {
        this.textPen = textPen;
    }

    void setAreaPen(AreaPen pen) {
        this.areaPen = pen;
    }

    void setSearchAreaPen(SearchAreaPen pen) {
        this.searchAreaPen = pen;
    }

    void setEraser(Eraser eraser) {
        this.eraser = eraser;
    }

    public void setAreaMarkPen(MarkAreaType markAreaType, Pen.MarkPen markPen) {
        if (markAreaType == null) {
            return;
        }
        areaMarkPens.put(markAreaType, markPen);
    }

    public Pen.MarkPen getAreaMarkPen(MarkAreaType markAreaType) {
        if (markAreaType == null) {
            return null;
        }
        if (areaMarkPens.containsKey(markAreaType)) {
            return areaMarkPens.get(markAreaType);
        } else {
            return null;
        }
    }

    public void removeAreaMarkPen(MarkAreaType markAreaType) {
        if (markAreaType == null) {
            return;
        }
        if (areaMarkPens.containsKey(markAreaType)) {
            areaMarkPens.remove(markAreaType);
        }
    }

    /**
     * 触摸事件,生成注释
     */
    boolean onTouch(MotionEvent event) {
        if (pdfView.getFunction() == PDFView.Function.PEN) {
            return onPenTouch(event);
        } else if (pdfView.getFunction() == PDFView.Function.ERASER) {
            return onEraserTouch(event);
        } else if (pdfView.getFunction() == PDFView.Function.MARK) {
            return onMarkTouch(event);
        }
        return false;
    }

    /**
     * 搜素的容差范围
     */
    int getSearchRange(int pageIndex) {
        return Math.min(
                pdfView.pdfiumCore.getPageWidthPoint(pdfView.pdfFile.pdfDocument, pageIndex),
                pdfView.pdfiumCore.getPageHeightPoint(pdfView.pdfFile.pdfDocument, pageIndex)
        ) / 10;
    }

    boolean isActiveAreaSelect() {
        return downTargetTextInfo != null;
    }

    /**
     * 区域选中模式
     */
    boolean onAreaTouch(MotionEvent event, OnAreaTouchListener onAreaTouchListener) {
        if (event.getPointerId(event.getActionIndex()) != 0) {
            return false;
        }
        if (areaSelect) {
            return true;
        }
        if (areaPen == null) {
            areaPen = PenBuilder.areaPenBuilder().setColor(Color.parseColor("#66666666")).build();
        }
        if (!checkActiveAreaAnnotation(event)) {
            return true;//返回true,即使触摸点没有文字,也是正常消费了事件的
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                currentTargetTextInfo = getTargetTextIndex(event.getX(), event.getY());
                if (currentTargetTextInfo == null) {
                    return true;
                }
                int currentTextIndex = currentTargetTextInfo.getTextIndex();
                if (areaMarkAnnotation.page == currentTargetTextInfo.getPage() && currentTargetTextInfo.isInPage()) {
                    if (currentTextIndex == -1) {
                        return true;
                    }
                    int start = areaMarkAnnotation.startIndex;
                    int count = start - currentTextIndex;
                    boolean desc = count > 0;
                    count = Math.abs(count);
                    areaRects.clear();
                    for (int i = 0; i <= count; i++) {
                        areaRects.add(pdfView.pdfiumCore.getTextRect(currentTargetTextInfo.getPagePtr(), start + (desc ? -1 * i : i)));
                    }
                    if (desc) {
                        Collections.reverse(areaRects);
                    }
                    areaMarkAnnotation.update(currentTextIndex, areaRects);
                    pdfView.redrawRenderingView();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (onAreaTouchListener != null) {
                    if (areaPen == null || currentTargetTextInfo == null || areaMarkAnnotation.page != currentTargetTextInfo.getPage() || !currentTargetTextInfo.isInPage()) {
                        return true;
                    }
                    float zoom = pdfView.getZoom();
                    float translateX, translateY;
                    SizeF size = pdfView.pdfFile.getPageSize(currentTargetTextInfo.getPage());
                    if (pdfView.isSwipeVertical()) {
                        float maxWidth = pdfView.pdfFile.getMaxPageWidth();
                        float localTranslationX = pdfView.toCurrentScale(maxWidth - size.getWidth()) / 2;
                        translateX = pdfView.getCurrentXOffset() + localTranslationX;
                        translateY = pdfView.getCurrentYOffset() + pdfView.pdfFile.getPageOffset(currentTargetTextInfo.getPage(), zoom);
                    } else {
                        float maxHeight = pdfView.pdfFile.getMaxPageHeight();
                        float localTranslationY = pdfView.toCurrentScale(maxHeight - size.getHeight()) / 2;
                        translateY = pdfView.getCurrentYOffset() + localTranslationY;
                        translateX = pdfView.getCurrentXOffset() + pdfView.pdfFile.getPageOffset(currentTargetTextInfo.getPage(), zoom);
                    }

                    RectF startRect = areaPen.getStartRectF(areaRects, pdfView, currentTargetTextInfo.getPage(), currentTargetTextInfo.getScale());
                    RectF endRect = areaPen.getEndRectF(areaRects, pdfView, currentTargetTextInfo.getPage(), currentTargetTextInfo.getScale());
                    if (startRect == null || endRect == null) {
                        return true;
                    }
                    startRect.left *= zoom;
                    startRect.top *= zoom;
                    startRect.right *= zoom;
                    startRect.bottom *= zoom;
                    endRect.left *= zoom;
                    endRect.top *= zoom;
                    endRect.right *= zoom;
                    endRect.bottom *= zoom;

                    float targetViewSize = areaPen.getTargetViewSize() * zoom;

                    float startTopLeftX = startRect.left + translateX - targetViewSize;
                    float startTopLeftY = startRect.bottom + translateY;
                    float startBottomRightX = startTopLeftX + targetViewSize;
                    float startBottomRightY = startTopLeftY + targetViewSize;

                    float endTopLeftX = endRect.right + translateX;
                    float endTopLeftY = endRect.bottom + translateY;
                    float endBottomRightX = endTopLeftX + targetViewSize;
                    float endBottomRightY = endTopLeftY + targetViewSize;

                    startTargetRect = new RectF(startTopLeftX, startTopLeftY, startBottomRightX, startBottomRightY);
                    endTargetRect = new RectF(endTopLeftX, endTopLeftY, endBottomRightX, endBottomRightY);

                    areaSelect = true;
                    onAreaTouchListener.onAreaSelect(startRect, endRect, translateX, translateY, targetViewSize, getSelectMarkAreaTypes());
                }
                break;
        }
        return true;
    }


    boolean isMoveEnd(boolean isStartTarget) {
        boolean isReverse = areaMarkAnnotation.startIndex - areaMarkAnnotation.endIndex > 0;
        boolean isMoveEnd = false;
        if ((!isStartTarget && !isReverse) || (isStartTarget && isReverse)) {
            isMoveEnd = true;
        }
        return isMoveEnd;
    }

    /**
     * 拖动游标重新选择区域
     */
    boolean onAreaTouchWithReTouch(MotionEvent event, OnAreaTouchListener onAreaTouchListener, boolean isMoveEnd) {
        if (event.getPointerId(event.getActionIndex()) != 0) {
            return false;
        }
        if (!areaSelect) {
            return true;
        }
        if (areaPen == null) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                TargetTextInfo theTargetTextInfo = getTargetTextIndex(event.getX(), event.getY());
                if (theTargetTextInfo == null) {
                    return true;
                }
                if (isMoveEnd) {
                    currentTargetTextInfo = theTargetTextInfo;
                }
                int currentTextIndex = theTargetTextInfo.getTextIndex();
                if (areaMarkAnnotation.page == theTargetTextInfo.getPage() && theTargetTextInfo.isInPage()) {
                    if (currentTextIndex == -1) {
                        return true;
                    }
                    if (!isMoveEnd) {
                        int start = currentTextIndex;
                        int count = start - areaMarkAnnotation.endIndex;
                        boolean desc = count > 0;
                        count = Math.abs(count);
                        areaRects.clear();
                        for (int i = 0; i <= count; i++) {
                            areaRects.add(pdfView.pdfiumCore.getTextRect(theTargetTextInfo.getPagePtr(), start + (desc ? -1 * i : i)));
                        }
                        if (desc) {
                            Collections.reverse(areaRects);
                        }
                        areaMarkAnnotation.updateAll(currentTextIndex, areaMarkAnnotation.endIndex, areaRects);
                    } else {
                        int start = areaMarkAnnotation.startIndex;
                        int count = start - currentTextIndex;
                        boolean desc = count > 0;
                        count = Math.abs(count);
                        areaRects.clear();
                        for (int i = 0; i <= count; i++) {
                            areaRects.add(pdfView.pdfiumCore.getTextRect(theTargetTextInfo.getPagePtr(), start + (desc ? -1 * i : i)));
                        }
                        if (desc) {
                            Collections.reverse(areaRects);
                        }
                        areaMarkAnnotation.update(currentTextIndex, areaRects);
                    }
                    pdfView.redrawRenderingView();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (onAreaTouchListener != null) {
                    if (areaPen == null || currentTargetTextInfo == null || areaMarkAnnotation.page != currentTargetTextInfo.getPage() || !currentTargetTextInfo.isInPage()) {
                        return true;
                    }
                    float zoom = pdfView.getZoom();
                    float translateX, translateY;
                    SizeF size = pdfView.pdfFile.getPageSize(currentTargetTextInfo.getPage());
                    if (pdfView.isSwipeVertical()) {
                        float maxWidth = pdfView.pdfFile.getMaxPageWidth();
                        float localTranslationX = pdfView.toCurrentScale(maxWidth - size.getWidth()) / 2;
                        translateX = pdfView.getCurrentXOffset() + localTranslationX;
                        translateY = pdfView.getCurrentYOffset() + pdfView.pdfFile.getPageOffset(currentTargetTextInfo.getPage(), zoom);
                    } else {
                        float maxHeight = pdfView.pdfFile.getMaxPageHeight();
                        float localTranslationY = pdfView.toCurrentScale(maxHeight - size.getHeight()) / 2;
                        translateY = pdfView.getCurrentYOffset() + localTranslationY;
                        translateX = pdfView.getCurrentXOffset() + pdfView.pdfFile.getPageOffset(currentTargetTextInfo.getPage(), zoom);
                    }

                    RectF startRect = areaPen.getStartRectF(areaRects, pdfView, currentTargetTextInfo.getPage(), currentTargetTextInfo.getScale());
                    RectF endRect = areaPen.getEndRectF(areaRects, pdfView, currentTargetTextInfo.getPage(), currentTargetTextInfo.getScale());
                    if (startRect == null || endRect == null) {
                        return true;
                    }
                    startRect.left *= zoom;
                    startRect.top *= zoom;
                    startRect.right *= zoom;
                    startRect.bottom *= zoom;
                    endRect.left *= zoom;
                    endRect.top *= zoom;
                    endRect.right *= zoom;
                    endRect.bottom *= zoom;

                    float targetViewSize = areaPen.getTargetViewSize() * zoom;
                    RectF theStartRect = startRect;
                    RectF theEndRect = endRect;
                    float startTopLeftX = theStartRect.left + translateX - targetViewSize;
                    float startTopLeftY = theStartRect.bottom + translateY;
                    float startBottomRightX = startTopLeftX + targetViewSize;
                    float startBottomRightY = startTopLeftY + targetViewSize;

                    float endTopLeftX = theEndRect.right + translateX;
                    float endTopLeftY = theEndRect.bottom + translateY;
                    float endBottomRightX = endTopLeftX + targetViewSize;
                    float endBottomRightY = endTopLeftY + targetViewSize;

                    startTargetRect = new RectF(startTopLeftX, startTopLeftY, startBottomRightX, startBottomRightY);
                    endTargetRect = new RectF(endTopLeftX, endTopLeftY, endBottomRightX, endBottomRightY);

                    //如果执行onAreaSelect,会导致popwindow销毁重建,导致触摸事件无法响应,所以需要另外方式通知popwindow位置更新
                    onAreaTouchListener.onReTouchAreaSelectUpdate(startRect, endRect, translateX, translateY, targetViewSize, getSelectMarkAreaTypes());
                }
                break;
        }
        return true;
    }


    RectF convertPdfPositionToScreenPosition(int page, RectF rectF) {
        if (rectF == null || pdfView.pdfFile == null) {
            return null;
        }
        SizeF size = pdfView.pdfFile.getPageSize(page);
        float zoom = pdfView.getZoom();
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        float scale = pdfSize.getWidth() / size.getWidth();

        rectF.left /= scale;
        rectF.top /= scale;
        rectF.right /= scale;
        rectF.bottom /= scale;
        rectF.left *= zoom;
        rectF.top *= zoom;
        rectF.right *= zoom;
        rectF.bottom *= zoom;
        float translateX, translateY;
        if (pdfView.isSwipeVertical()) {
            float maxWidth = pdfView.pdfFile.getMaxPageWidth();
            float localTranslationX = pdfView.toCurrentScale(maxWidth - size.getWidth()) / 2;
            translateX = pdfView.getCurrentXOffset() + localTranslationX;
            translateY = pdfView.getCurrentYOffset() + pdfView.pdfFile.getPageOffset(page, zoom);
        } else {
            float maxHeight = pdfView.pdfFile.getMaxPageHeight();
            float localTranslationY = pdfView.toCurrentScale(maxHeight - size.getHeight()) / 2;
            translateY = pdfView.getCurrentYOffset() + localTranslationY;
            translateX = pdfView.getCurrentXOffset() + pdfView.pdfFile.getPageOffset(page, zoom);
        }
        float left = rectF.left + translateX;
        float top = rectF.top + translateY;
        float right = rectF.right + translateX;
        float bottom = rectF.bottom + translateY;
        return new RectF(left, top, right, bottom);
    }

    /**
     * 检查是否符合激活区域标记
     */
    boolean checkActiveAreaAnnotation(MotionEvent event) {
        if (areaMarkAnnotation == null) {
            float downX = event.getX();
            float downY = event.getY();
            TargetTextInfo targetTextInfo = getTargetTextIndex(downX, downY);
            if (targetTextInfo == null) {
                return false;
            }
            if (!targetTextInfo.isInPage() || targetTextInfo.getTextIndex() == -1) {//如果没有选中文字,舍弃按下的坐标,舍弃这次长按事件
                Log.e("activeAreaAnotation", "textIndex:" + targetTextInfo.getTextIndex() + ",isInPage:" + targetTextInfo.isInPage());
                return false;
            }
            areaDownX = downX;
            areaDownY = downY;
            downTargetTextInfo = targetTextInfo;
            areaMarkAnnotation = new MarkAnnotation(downTargetTextInfo.getPage(), downTargetTextInfo.getPageSize(), areaPen, MarkAreaType.AREACHOOSE, downTargetTextInfo.getTextIndex(), downTargetTextInfo.getTextIndex(), pdfView.pdfiumCore.getTextRect(downTargetTextInfo.getPagePtr(), downTargetTextInfo.getTextIndex()));
        }
        return true;
    }

    /**
     * 区域选中清除
     */
    void clearArea() {
        areaDownX = 0;
        areaDownY = 0;
        areaSelect = false;
        downTargetTextInfo = null;
        currentTargetTextInfo = null;
        areaMarkAnnotation = null;
        areaRects.clear();
        startTargetRect = null;
        endTargetRect = null;
        pdfView.redrawRenderingView();
    }

    String textOutSideCropCheck(float x,float y,String text,float zoom){
        if(text.length()==0){
            return null;
        }
        Bitmap bitmap = pdfView.getTextRemarkBitmapCache("",text, textPen.getColor(), zoom,false);
        if (bitmap == null) {
            return null;
        }
        int[] coordLeftTop = CoordinateUtils.getPdfCoordinate(pdfView,x, y);
        int[] coordRightBottom = CoordinateUtils.getPdfCoordinate(pdfView, x + bitmap.getWidth(), y + bitmap.getHeight());
        if (coordLeftTop == null || coordRightBottom == null) {
            return null;
        }
        if (coordLeftTop[0] != coordRightBottom[0]) {
            if(text.length()>1){
              text=textOutSideCropCheck(x,y,text.substring(0,text.length() - 1),zoom);
            }
        }
        return text;
    }

    void saveTextPenAnnotation(TextRemarkInfo textRemarkInfo,boolean outSideCrop) {
        if (textPen == null) {
            textPen = PenBuilder.textPenBuilder().setColor(Color.parseColor("#66666666")).setFontSize(pdfView.getEditTextRemarkFontSize()).build();
        }
        if(outSideCrop){
           String text= textOutSideCropCheck(textRemarkInfo.getX(),textRemarkInfo.getY(),textRemarkInfo.getData(),textRemarkInfo.getZoom());
           if(text==null){
               return;
           }
           textRemarkInfo.setData(text);
        }
        Bitmap bitmap = pdfView.getTextRemarkBitmapCache(textRemarkInfo.getKey(),textRemarkInfo.getData(), textPen.getColor(), textRemarkInfo.getZoom(),true);
        if (bitmap == null) {
            return;
        }
        textRemarkInfo.setWidth(textRemarkInfo.getWidth());
        textRemarkInfo.setHeight(textRemarkInfo.getHeight());
        int[] coordLeftTop = CoordinateUtils.getPdfCoordinate(pdfView, textRemarkInfo.getX(), textRemarkInfo.getY());
        int[] coordRightBottom = CoordinateUtils.getPdfCoordinate(pdfView, textRemarkInfo.getX() + bitmap.getWidth(), textRemarkInfo.getY() + bitmap.getHeight());
        if (coordLeftTop == null || coordRightBottom == null) {
            return;
        }
        if (coordLeftTop[0] != coordRightBottom[0]) {
            Log.e("SaveTextPenAnnotation", "The text left top and right bottom not in same page");
            return;
        }
        SizeF leftTopPdfSize = CoordinateUtils.toPdfPointCoordinate(pdfView, coordLeftTop[0], coordLeftTop[1], coordLeftTop[2]);
        SizeF rightBottomPdfSize = CoordinateUtils.toPdfPointCoordinate(pdfView, coordRightBottom[0], coordRightBottom[1], coordRightBottom[2]);

        Size size = CoordinateUtils.getPdfPageSize(pdfView, coordLeftTop[0]);
        SizeF displaySize = pdfView.getPageSize(coordLeftTop[0]);
        float scale = size.getWidth() / displaySize.getWidth();

        textRemarkInfo.setLeftTopPdfSize(leftTopPdfSize);
        textRemarkInfo.setRightBottomPdfSize(rightBottomPdfSize);
        textRemarkInfo.setScale(scale);

        TextAnnotation textAnnotation = new TextAnnotation(textRemarkInfo.getPage(), size, textPen);
        textAnnotation.data = textRemarkInfo;
        textAnnotation.drawed = false;
        addTheAnnotation(textAnnotation, true);
        pdfView.redrawRenderingView();
    }

    /**
     * 保存画笔绘制内容
     */
    void savePenDrawing() {
        if (drawingPenAnnotations.size() == 0) {
            return;
        }
        for (PenAnnotation penAnnotation : drawingPenAnnotations) {
            if (penAnnotation.data.size() >= 1) {
                penAnnotation.drawed = false;
                addTheAnnotation(penAnnotation, true);
            }
        }
        drawingPenAnnotations.clear();
        drawingPenAnnotation = null;
        pdfView.redrawRenderingView();
    }

    /**
     * 清理画笔绘制内容
     */
    void cancelPenDrawing() {
        drawingPenAnnotations.clear();
        drawingPenAnnotation = null;
        pdfView.redrawRenderingView();
    }

    /**
     * 清理画笔绘制内容
     */
    void clearPenDrawing() {
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllPenAnnotation(pdfView.getCurrentPage());
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            removeTheAnnotation(baseAnnotation, true);
        }
        drawingPenAnnotations.clear();
        drawingPenAnnotation = null;
        pdfView.redrawRenderingView();
    }

    /**
     * 获取坐标对应的文字信息
     */
    private TargetTextInfo getTargetTextIndex(float x, float y) {
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return null;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);
        SizeF displaySize = pdfView.getPageSize(coord[0]);
        float scale = size.getWidth() / displaySize.getWidth();
        Rect rect = new Rect(0, 0, size.getWidth(), size.getHeight());
        boolean inPage = rect.contains(coord[1], coord[2]);
//        if (!inPage) {
//            return null;
//        }
        Long pagePtr = pdfView.pdfFile.pdfDocument.getTextPagesPtr(coord[0]);
        if (pagePtr == null) {
            return null;
        }
        int searchRange = getSearchRange(coord[0]);
        SizeF pdfCoordinate = CoordinateUtils.toPdfPointCoordinate(pdfView, coord[0], coord[1], coord[2]);
        int textIndex = pdfView.pdfiumCore.getCharIndexAtPos(pagePtr, pdfCoordinate.getWidth(), pdfCoordinate.getHeight(), searchRange, searchRange);

        return new TargetTextInfo(coord[0], size, textIndex, pagePtr, inPage, scale);
    }

    /**
     * 选中区域画备注
     */
    boolean drawSelectAreaWithMarkAreaType(MarkAreaType markAreaType) {
        if (!areaSelect || downTargetTextInfo == null || currentTargetTextInfo == null || areaMarkAnnotation == null || areaRects.size() == 0) {
            return false;
        }
        Pen.MarkPen markPen = getAreaMarkPen(markAreaType);
        if (markPen == null) {
            Log.e(TAG, "don't have mark pen");
            return false;
        }
        MarkAnnotation markAnnotation = new MarkAnnotation(areaMarkAnnotation.page, areaMarkAnnotation.pageSize, markPen, markAreaType, areaMarkAnnotation.startIndex, areaMarkAnnotation.startIndex, pdfView.pdfiumCore.getTextRect(downTargetTextInfo.getPagePtr(), downTargetTextInfo.getTextIndex()));
        markAnnotation.update(areaMarkAnnotation.endIndex, areaRects);
        if (markAnnotation.data.size() >= 1) {
            markAnnotation.drawed = false;
            addTheAnnotation(markAnnotation, true);
        }
        pdfView.redrawRenderingView();
        return true;
    }

    /**
     * 搜索区域提示
     */
    boolean drawSearchArea(SearchTextInfo searchTextInfo) {
        if (searchTextInfo == null || searchTextInfo.getData().size() == 0) {
            return false;
        }
        if (searchAreaPen == null) {
            searchAreaPen = PenBuilder.searchAreaPenBuilder().setColor(Color.parseColor("#66666666")).build();
        }
        int page = searchTextInfo.getPage();
        Size size = CoordinateUtils.getPdfPageSize(pdfView, page);
        searchMarkAnnotation = new MarkAnnotation(page, size, searchAreaPen, MarkAreaType.AREACHOOSE, searchTextInfo.getStart(), searchTextInfo.getEnd(), new RectF());
        searchMarkAnnotation.updateRects(searchTextInfo.getData());
        searchMarkAnnotation.drawed = false;
        pdfView.redrawRenderingView();
        return true;
    }

    /**
     * 清理搜索提示内容
     */
    void clearSearchArea() {
        searchMarkAnnotation = null;
        pdfView.redrawRenderingView();
    }

    /**
     * 获取当前页的所有对应区域类型的备注
     */
    List<BaseAnnotation> getCurrentPageAllAnnotation(MarkAreaType markAreaType, int page) {
        List<BaseAnnotation> targetAnnotations = new ArrayList<>();
        List<BaseAnnotation> pageAnnotations = annotations.get(page);
        if (pageAnnotations == null) {
            return targetAnnotations;
        }
        for (int i = 0; i < pageAnnotations.size(); i++) {
            BaseAnnotation baseAnnotation = pageAnnotations.get(i);
            if (markAreaType == null) {
                targetAnnotations.add(baseAnnotation);
            } else {
                if (baseAnnotation instanceof MarkAnnotation && markAreaType == ((MarkAnnotation) baseAnnotation).markAreaType) {
                    targetAnnotations.add(baseAnnotation);
                }
            }
        }
        return targetAnnotations;
    }

    List<BaseAnnotation> getCurrentPageAllPenAnnotation(int page) {
        List<BaseAnnotation> targetAnnotations = new ArrayList<>();
        List<BaseAnnotation> pageAnnotations = annotations.get(page);
        if (pageAnnotations == null) {
            return targetAnnotations;
        }
        for (int i = 0; i < pageAnnotations.size(); i++) {
            BaseAnnotation baseAnnotation = pageAnnotations.get(i);
            if (baseAnnotation instanceof PenAnnotation) {
                targetAnnotations.add(baseAnnotation);
            }
        }
        return targetAnnotations;
    }

    List<BaseAnnotation> getCurrentPageAllTextPenAnnotation(int page) {
        List<BaseAnnotation> targetAnnotations = new ArrayList<>();
        List<BaseAnnotation> pageAnnotations = annotations.get(page);
        if (pageAnnotations == null) {
            return targetAnnotations;
        }
        for (int i = 0; i < pageAnnotations.size(); i++) {
            BaseAnnotation baseAnnotation = pageAnnotations.get(i);
            if (baseAnnotation instanceof TextAnnotation) {
                targetAnnotations.add(baseAnnotation);
            }
        }
        return targetAnnotations;
    }

    /**
     * 选中区域清理对应备注
     */
    boolean cancelSelectAreaAnnotation(MarkAreaType markAreaType) {
        if (!areaSelect || areaMarkAnnotation == null || areaRects.size() == 0) {
            return false;
        }
        int startIndex = areaMarkAnnotation.startIndex;
        int endIndex = areaMarkAnnotation.endIndex;
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllAnnotation(markAreaType, areaMarkAnnotation.page);
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            if (baseAnnotation instanceof MarkAnnotation) {
                MarkAnnotation markAnnotation = (MarkAnnotation) baseAnnotation;
                if (isAreaIntersect(startIndex, endIndex, markAnnotation.startIndex, markAnnotation.endIndex)) {
                    removeTheAnnotation(baseAnnotation, true);
                }
            }
        }
        pdfView.redrawRenderingView();
        return true;
    }


    /**
     * 获得选中的区域标记类型
     */
    List<MarkAreaType> getSelectMarkAreaTypes() {
        List<MarkAreaType> markAreaTypes = new ArrayList<>();
        if (!areaSelect || areaMarkAnnotation == null || areaRects.size() == 0) {
            return markAreaTypes;
        }
        int startIndex = areaMarkAnnotation.startIndex;
        int endIndex = areaMarkAnnotation.endIndex;
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllAnnotation(null, areaMarkAnnotation.page);
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            if (baseAnnotation instanceof MarkAnnotation) {
                MarkAnnotation markAnnotation = (MarkAnnotation) baseAnnotation;
                if (isAreaIntersect(startIndex, endIndex, markAnnotation.startIndex, markAnnotation.endIndex)) {
                    if (!markAreaTypes.contains(markAnnotation.markAreaType)) {
                        markAreaTypes.add(markAnnotation.markAreaType);
                    }
                }
            }
        }
        return markAreaTypes;
    }

    /**
     * 清除当前页所有备注
     */
    void clearPage() {
        if (!areaSelect || areaMarkAnnotation == null || areaRects.size() == 0) {
            return;
        }
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllAnnotation(null, areaMarkAnnotation.page);
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            removeTheAnnotation(baseAnnotation, true);
        }
        pdfView.redrawRenderingView();
    }

    /**
     * 判断区间是否存在重叠
     */
    boolean isAreaIntersect(int a1, int a2, int b1, int b2) {
        if (a1 > a2) {
            int a = a2;
            a2 = a1;
            a1 = a;
        }
        if (b1 > b2) {
            int b = b2;
            b2 = b1;
            b1 = b;
        }
        if (a2 < b1 || a1 > b2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 生成画笔注释
     */
    private boolean onPenTouch(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) != 0) {
            return false;
        }

        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return false;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);
        Rect rect = new Rect(0, 0, size.getWidth(), size.getHeight());
        boolean inPage = rect.contains(coord[1], coord[2]);
        if (action == MotionEvent.ACTION_DOWN) {
            if (!inPage) {
                return false;
            }
            if (pen instanceof Pen.WritePen) {
                this.drawingPenAnnotation = new PenAnnotation(coord[0], size, (Pen.WritePen) pen);
            } else {
                return false;
            }
            ((PenAnnotation) drawingPenAnnotation).data.add(CoordinateUtils.toPdfPointCoordinate(pdfView, coord[0], coord[1], coord[2]));
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = Math.abs(x - penTouchX);
            float dy = Math.abs(penTouchY - y);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                if (drawingPenAnnotation != null) {
                    PenAnnotation penAnnotation = (PenAnnotation) drawingPenAnnotation;
                    if (drawingPenAnnotation.page == coord[0] && inPage) {
                        penAnnotation.data.add(CoordinateUtils.toPdfPointCoordinate(pdfView, coord[0], coord[1], coord[2]));
                    } else {
                        if (penAnnotation.data.size() > 1) {
                            addToDrawingPenAnnotations(drawingPenAnnotation);
                        }
                        drawingPenAnnotation = null;
                    }
                    pdfView.redrawRenderingView();
                }
                penTouchX=x;
                penTouchY=y;
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (drawingPenAnnotation != null) {
                PenAnnotation penAnnotation = (PenAnnotation) drawingPenAnnotation;
                if (drawingPenAnnotation.page == coord[0] && inPage) {
                    penAnnotation.data.add(CoordinateUtils.toPdfPointCoordinate(pdfView, coord[0], coord[1], coord[2]));
                }
                if (penAnnotation.data.size() > 1) {
                    addToDrawingPenAnnotations(drawingPenAnnotation);
                }
                drawingPenAnnotation = null;
                pdfView.redrawRenderingView();
            }
        }
        return true;
    }

    /**
     * 生成橡皮擦
     */
    private boolean onEraserTouch(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) != 0 || eraser == null) {//不处理第一根手指以外的事件
            return false;
        }
        int action = event.getActionMasked();
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, event.getX(), event.getY());
        if (coord == null) {
            return false;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);
        SizeF displaySize = pdfView.getPageSize(coord[0]);
        float scale = size.getWidth() / displaySize.getWidth();
        Rect rect = new Rect(0, 0, size.getWidth(), size.getHeight());
        boolean inPage = rect.contains(coord[1], coord[2]);
        if (action == MotionEvent.ACTION_DOWN) {
            if (!inPage) {
                return false;
            }
            eraserPage = coord[0];
            eraser.move(coord[1], coord[2]);
            removeTheAnnotation(eraser.erase(annotations.get(eraserPage), scale, pdfView.getZoom(), pdfView), true);
            pdfView.redrawRenderingView();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (eraserPage == coord[0]) {
                eraser.move(coord[1], coord[2]);
                removeTheAnnotation(eraser.erase(annotations.get(eraserPage), scale, pdfView.getZoom(), pdfView), true);
                pdfView.redrawRenderingView();
            } else {
                eraserPage = -1;
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            eraserPage = -1;
            pdfView.redrawRenderingView();
        }
        return true;
    }

    /**
     * 生成标记
     */
    private boolean onMarkTouch(MotionEvent event) {
        if (event.getPointerId(event.getActionIndex()) != 0) {
            return false;
        }
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE &&
                action != MotionEvent.ACTION_CANCEL && action != MotionEvent.ACTION_UP
                && action != MotionEvent.ACTION_POINTER_UP
        ) {
            return false;
        }
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, event.getX(), event.getY());
        if (coord == null) {
            return false;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);
        Rect rect = new Rect(0, 0, size.getWidth(), size.getHeight());
        boolean inPage = rect.contains(coord[1], coord[2]);
        if (action == MotionEvent.ACTION_DOWN && !inPage) {
            return false;
        }
        Long pagePtr = pdfView.pdfFile.pdfDocument.getTextPagesPtr(coord[0]);
        if (pagePtr == null) {
            return false;
        }
        int searchRange = getSearchRange(coord[0]);
        //转换为pdf页坐标
        SizeF pdfCoordinate = CoordinateUtils.toPdfPointCoordinate(pdfView, coord[0], coord[1], coord[2]);
        //检索的文本下标
        int textIndex = pdfView.pdfiumCore.getCharIndexAtPos(pagePtr, pdfCoordinate.getWidth(), pdfCoordinate.getHeight(), searchRange, searchRange);
        if (action == MotionEvent.ACTION_DOWN) {
            if (textIndex == -1) {
                return false;
            }
            if (pen instanceof Pen.MarkPen) {
                this.drawingAnnotation = new MarkAnnotation(coord[0], size, (Pen.MarkPen) pen, MarkAreaType.EXTRA, textIndex, textIndex, pdfView.pdfiumCore.getTextRect(pagePtr, textIndex));
            } else {
                return false;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (drawingAnnotation != null) {
                if (drawingAnnotation.page == coord[0] && inPage) {
                    if (textIndex == -1) {
                        return true;
                    } else {
                        int start = ((MarkAnnotation) drawingAnnotation).startIndex;
                        int count = start - textIndex;
                        //是否反序
                        boolean desc = count > 0;
                        count = Math.abs(count);
                        ArrayList<RectF> rects = new ArrayList<>();
                        for (int i = 0; i <= count; i++) {
                            rects.add(pdfView.pdfiumCore.getTextRect(pagePtr, start + (desc ? -1 * i : i)));
                        }
                        //如果倒序选，反转排序。保证列表为正序选择
                        if (desc) {
                            Collections.reverse(rects);
                        }
                        ((MarkAnnotation) drawingAnnotation).update(textIndex, rects);
                    }
                } else {
                    if (((MarkAnnotation) drawingAnnotation).data.size() >= 1) {
                        drawingAnnotation.drawed = false;
                        addTheAnnotation(drawingAnnotation, true);
                    }
                    drawingAnnotation = null;
                }
                pdfView.redrawRenderingView();
            }
        } else {
            if (drawingAnnotation != null) {
                if (drawingAnnotation.page == coord[0] && inPage && textIndex != -1) {
                    int start = ((MarkAnnotation) drawingAnnotation).startIndex;
                    int count = ((MarkAnnotation) drawingAnnotation).startIndex - textIndex;
                    boolean desc = count > 0;
                    count = Math.abs(count);
                    ArrayList<RectF> rectFS = new ArrayList<>();
                    for (int i = 0; i <= count; i++) {
                        rectFS.add(pdfView.pdfiumCore.getTextRect(pagePtr, start + (desc ? -1 * i : i)));
                    }
                    if (desc) {
                        Collections.reverse(rectFS);
                    }
                    ((MarkAnnotation) drawingAnnotation).update(textIndex, rectFS);
                }
                if (((MarkAnnotation) drawingAnnotation).data.size() >= 1) {
                    drawingAnnotation.drawed = false;
                    addTheAnnotation(drawingAnnotation, true);
                }
                drawingAnnotation = null;
                pdfView.redrawRenderingView();
            }
        }
        return true;
    }

    public void loadData(MarkAnnotation markAnnotation) {
        if (markAnnotation == null || (markAnnotation.startIndex == 0 && markAnnotation.endIndex == 0)) {
            return;
        }
        int start = markAnnotation.startIndex;
        int count = markAnnotation.startIndex - markAnnotation.endIndex;
        boolean desc = count > 0;
        count = Math.abs(count);
        ArrayList<RectF> rects = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            rects.add(pdfView.pdfiumCore.getTextRect(pdfView.pdfFile.pdfDocument.getTextPagesPtr(markAnnotation.page), start + (desc ? -1 * i : i)));
        }
        if (desc) {
            Collections.reverse(rects);
        }
        markAnnotation.updateRects(rects);
    }

    /**
     * 添加到画笔缓存
     */
    private void addToDrawingPenAnnotations(PenAnnotation annotation) {
        drawingPenAnnotations.add(annotation);
    }

    /**
     * 添加到缓存
     */
    private void addTheAnnotation(@Nullable BaseAnnotation annotation, boolean needNotify) {
        if (annotation == null) {
            return;
        }
        List<BaseAnnotation> list = annotations.get(annotation.page);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(annotation);
        annotations.put(annotation.page, list);

        if (annotationListener != null && needNotify) {
            annotationListener.onAnnotationAdd(annotation);
        }
    }

    /**
     * 从缓存删除
     */
    private void removeTheAnnotation(@Nullable BaseAnnotation annotation, boolean needNotify) {
        if (annotation == null) {
            return;
        }
        List<BaseAnnotation> list = annotations.get(annotation.page);
        if (list == null) {
            return;
        }
        list.remove(annotation);
        Bitmap bitmap = pdfView.annotationDrawManager.cache.get(annotation.page);
        if (bitmap != null && !bitmap.isRecycled()) {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//清空绘画的注释
        }
        for (BaseAnnotation annotation1 : annotations.get(annotation.page)) {
            annotation1.drawed = false;
        }
        if (annotationListener != null && needNotify) {
            annotationListener.onAnnotationRemove(annotation);
        }
    }

    public void resetAnnotationDraw(@Nullable BaseAnnotation annotation){
        Bitmap bitmap = pdfView.annotationDrawManager.cache.get(annotation.page);
        if (bitmap != null && !bitmap.isRecycled()) {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//清空绘画的注释
        }
        for (BaseAnnotation annotation1 : annotations.get(annotation.page)) {
            annotation1.drawed = false;
        }
    }

    public void addAnnotations(List<BaseAnnotation> data, boolean needNotify) {
        for (BaseAnnotation baseAnnotation : data) {
            addTheAnnotation(baseAnnotation, needNotify);
        }
        pdfView.redrawRenderingView();
    }

    boolean isInCancelArea(PenAnnotation penAnnotation, int x, int y) {
        Rect cancelAreaRect = penAnnotation.getCancelAreaRect();
        if (cancelAreaRect == null) {
            return false;
        }

        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return false;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);

        SizeF displaySize = pdfView.getPageSize(coord[0]);
        float scale = size.getWidth() / displaySize.getWidth();
        float tolerance = displaySize.getWidth() * 0.01f;

        int targetX = (int) (coord[1] / scale * pdfView.getZoom());
        int targetY = (int) (coord[2] / scale * pdfView.getZoom());

        RectF cancelAreaRectF = new RectF((int) (cancelAreaRect.left - tolerance), (int) (cancelAreaRect.top - tolerance), (int) (cancelAreaRect.right + tolerance), (int) (cancelAreaRect.bottom + tolerance));
        boolean inArea = cancelAreaRectF.contains(targetX, targetY);
        if (inArea) {
            return true;
        }
        return false;
    }


    boolean isInPenDrawArea(PenAnnotation penAnnotation, int x, int y) {
        RectF areaRect = penAnnotation.getAreaRect();
        if (areaRect == null) {
            return false;
        }

        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return false;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);

        SizeF displaySize = pdfView.getPageSize(coord[0]);
        float scale = size.getWidth() / displaySize.getWidth();

        int targetX = (int) (coord[1] / scale * pdfView.getZoom());
        int targetY = (int) (coord[2] / scale * pdfView.getZoom());

        RectF theAreaRectF = new RectF((int) areaRect.left, (int) areaRect.top, (int) areaRect.right, (int) areaRect.bottom);
        boolean inArea = theAreaRectF.contains(targetX, targetY);
        if (inArea) {
            return true;
        }
        return false;
    }

    List<PenAnnotation> getSelectPenAnnotations(int x, int y, boolean onlyTop) {
        List<PenAnnotation> selectPenAnnotations = new ArrayList<>();
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return selectPenAnnotations;
        }
        Size size = CoordinateUtils.getPdfPageSize(pdfView, coord[0]);
        float toleranceX = size.getWidth() * 0.02f;
        float toleranceY = size.getHeight() * 0.02f;
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllPenAnnotation(pdfView.getCurrentPage());
        Collections.reverse(pageAnnotations);
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            PenAnnotation penAnnotation = (PenAnnotation) baseAnnotation;
            RectF rectF = penAnnotation.getPdfAreaRect(pdfView);
            rectF.left = rectF.left - toleranceX;
            rectF.right = rectF.right + toleranceX;
            rectF.top = rectF.top - toleranceY;
            rectF.bottom = rectF.bottom + toleranceY;
            if (rectF.left < 0) {
                rectF.left = 0;
            }
            if (rectF.top < 0) {
                rectF.top = 0;
            }
            boolean inArea = rectF.contains(coord[1], coord[2]);
            if (inArea) {
                penAnnotation.setAreaRect(rectF);
                if (onlyTop) {
                    selectPenAnnotations.add(penAnnotation);
                    break;
                }
            }
        }
        return selectPenAnnotations;
    }


    List<TextAnnotation> getSelectTextPenAnnotations(int x, int y, boolean onlyTop) {
        List<TextAnnotation> selectTextAnnotations = new ArrayList<>();
        int[] coord = CoordinateUtils.getPdfCoordinate(pdfView, x, y);
        if (coord == null) {
            return selectTextAnnotations;
        }
        List<BaseAnnotation> pageAnnotations = getCurrentPageAllTextPenAnnotation(pdfView.getCurrentPage());
        Collections.reverse(pageAnnotations);
        for (BaseAnnotation baseAnnotation : pageAnnotations) {
            TextAnnotation textAnnotation = (TextAnnotation) baseAnnotation;
            RectF rectF = textAnnotation.getPdfAreaRect(pdfView);
            boolean inArea = rectF.contains(coord[1], coord[2]);
            if (inArea) {
                textAnnotation.setNeedHidden(false);
                textAnnotation.setAreaRect(rectF);
                if (onlyTop) {
                    selectTextAnnotations.add(textAnnotation);
                    break;
                }
            }
        }
        return selectTextAnnotations;
    }


    public void removeAnnotations(List<BaseAnnotation> data, boolean needNotify) {
        for (BaseAnnotation baseAnnotation : data) {
            removeTheAnnotation(baseAnnotation, needNotify);
        }
        pdfView.redrawRenderingView();
    }

    public List<BaseAnnotation> getAllAnnotation() {
        List<BaseAnnotation> all = new ArrayList<>();
        for (int i = 0; i < annotations.size(); i++) {
            List<BaseAnnotation> list = annotations.get(annotations.keyAt(i));
            all.addAll(list);
        }
        return all;
    }

    /**
     * 从缓存删除一页的注释
     */
    synchronized void removeAnnotation(@IntRange(from = 0) int page) {
        List<BaseAnnotation> list = annotations.get(page);
        if (list == null || list.size() == 0) {
            return;
        }
        list.clear();
        Bitmap bm = pdfView.annotationDrawManager.cache.get(page);
        if (bm == null || bm.isRecycled()) {
            return;
        }
        Canvas canvas = new Canvas(bm);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        pdfView.redrawRenderingView();
        if (annotationListener != null) {
            annotationListener.onAnnotationPageRemove(page);
        }
    }

    /**
     * 从缓存删除所有注释
     */
    synchronized void removeAnnotationAll() {
        annotations.clear();
        int size = pdfView.annotationDrawManager.cache.size();
        for (int i = 0; i < size; i++) {
            int key = pdfView.annotationDrawManager.cache.keyAt(i);
            Bitmap bm = pdfView.annotationDrawManager.cache.get(key);
            if (bm != null) {
                bm.recycle();
                bm = null;
            }
        }
        pdfView.redrawRenderingView();
        if (annotationListener != null) {
            annotationListener.onAnnotationAllRemove();
        }
    }

}