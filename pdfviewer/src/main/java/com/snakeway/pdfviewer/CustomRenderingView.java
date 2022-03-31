package com.snakeway.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.snakeway.pdfviewer.model.PagePart;

import java.util.ArrayList;
import java.util.List;


public class CustomRenderingView extends RelativeLayout {
    public final List<Integer> onDrawAnnotationPagesNums = new ArrayList<>(10);

    PDFView pdfView;

    Bitmap bitmap;

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

        for (PagePart part : pdfView.cacheManager.getPageParts()) {
            if (!onDrawAnnotationPagesNums.contains(part.getPage())) {
                onDrawAnnotationPagesNums.add(part.getPage());
            }
        }
        pdfView.annotationDrawManager.recycle(onDrawAnnotationPagesNums);
        for (Integer page : onDrawAnnotationPagesNums) {
            if(Math.abs(page-pdfView.getCurrentPage())<=pdfView.getAnnotationRenderingArea()){
                pdfView.annotationDrawManager.draw(canvas, page);
            }
        }
        onDrawAnnotationPagesNums.clear();
        canvas.translate(-currentXOffset, -currentYOffset);
    }

    public Bitmap getRenderingBitmap(int page){
        int width = getWidth();
        int height = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        if(!pdfView.annotationDrawManager.drawAnnotation(canvas, page)) {
            return null;
        }
        return bitmap;
    }

}


