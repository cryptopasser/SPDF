package com.snakeway.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdfviewer.model.PagePart;

import java.util.ArrayList;
import java.util.List;


public class CustomRenderingView extends RelativeLayout {
    public final static int RENDERING_AREA = 2;//渲染区间,当前页的前后页数

    PDFView pdfView;

    public CustomRenderingView(Context context, AttributeSet set) {
        super(context, set);
        setWillNotDraw(false);
    }

    public void initPdfView(PDFView pdfView) {
        this.pdfView = pdfView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        if (pdfView.isAntialiasing()) {
            canvas.setDrawFilter(pdfView.getAntialiasFilter());
        }
        Drawable bg = getBackground();
        if (bg == null) {
            canvas.drawColor(Color.TRANSPARENT);
        } else {
            bg.draw(canvas);
        }
        if (pdfView.isRecycled()) {
            return;
        }
        if (pdfView.getState() != PDFView.State.SHOWN) {
            return;
        }
        float currentXOffset = pdfView.getCurrentXOffset();
        float currentYOffset = pdfView.getCurrentYOffset();
        canvas.translate(currentXOffset, currentYOffset);

//        for (PagePart part : pdfView.cacheManager.getPageParts()) {
//            if (!onDrawAnnotationPagesNums.contains(part.getPage())) {
//                onDrawAnnotationPagesNums.add(part.getPage());
//            }
//        }
//        pdfView.annotationDrawManager.recycle(onDrawAnnotationPagesNums);
//        for (Integer page : onDrawAnnotationPagesNums) {
//            if(Math.abs(page-pdfView.getCurrentPage())<=pdfView.getAnnotationRenderingArea()){
//                pdfView.annotationDrawManager.draw(canvas, page);
//            }
//        }
//        onDrawAnnotationPagesNums.clear();
//        canvas.translate(-currentXOffset, -currentYOffset);
        List<Integer> onDrawAnnotationPages = new ArrayList<>();
        int startPage = pdfView.getCurrentPage() - RENDERING_AREA;
        int endPage = pdfView.getCurrentPage() + RENDERING_AREA;
        if (startPage < 0) {
            startPage = 0;
        }
        if (endPage > pdfView.getPageCount() - 1) {
            endPage = pdfView.getPageCount() - 1;
        }
        for (int i = startPage; i < endPage; i++) {
            onDrawAnnotationPages.add(i);
        }
        for (Integer page : onDrawAnnotationPages) {
            if (Math.abs(page - pdfView.getCurrentPage()) <= pdfView.getAnnotationRenderingArea()) {
                pdfView.annotationDrawManager.draw(canvas, page);
            }
        }
        canvas.translate(-currentXOffset, -currentYOffset);
    }

    public Bitmap getRenderingBitmap(int page, int targetWidth) {
        Size pdfSize = pdfView.pdfFile.originalPageSizes.get(page);
        float ratio = (float) pdfSize.getWidth() / targetWidth;
        int height = (int) ((float) pdfSize.getHeight() / ratio);
        Bitmap bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        if (!pdfView.annotationDrawManager.drawAnnotation(canvas, targetWidth, page)) {
            return null;
        }
        return bitmap;
    }

}


