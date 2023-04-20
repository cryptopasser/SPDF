package com.snakeway.pdfviewer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;


public class CustomPenDrawingView extends RelativeLayout {

    PDFView pdfView;

    boolean isHaveDrawing;

    public CustomPenDrawingView(Context context) {
        this(context, null, 0);
    }

    public CustomPenDrawingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomPenDrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomPenDrawingView, defStyleAttr, 0);
        isHaveDrawing = typedArray.getBoolean(R.styleable.CustomPenDrawingView_isHaveDrawing, false);
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
        //绘制已经抬起手指后的路径,不需要清理画布
        pdfView.annotationDrawManager.drawPenDrawing(canvas, pdfView.getCurrentPage(),isHaveDrawing);
        canvas.translate(-currentXOffset, -currentYOffset);
    }

}


