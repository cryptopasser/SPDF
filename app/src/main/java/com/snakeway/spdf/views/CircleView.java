package com.snakeway.spdf.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.snakeway.spdf.R;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/9 17:11
 */
public class CircleView extends View {
    Context context;

    boolean checked;
    int backgroundColor;
    float innerCirclePercent;
    int borderColor;
    float borderWidth;

    Paint paint;

    public CircleView(Context context) {
        this(context, null, 0);
    }

    public CircleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleView, defStyleAttr, 0);
        checked = typedArray.getBoolean(R.styleable.CircleView_checked, false);
        backgroundColor = typedArray.getColor(R.styleable.CircleView_backgroundColor, Color.BLACK);
        innerCirclePercent = typedArray.getFloat(R.styleable.CircleView_innerCirclePercent, 0.8F);
        borderColor = typedArray.getColor(R.styleable.CircleView_borderColor, Color.WHITE);
        borderWidth = typedArray.getDimension(R.styleable.CircleView_borderWidth, 8);
        init();
    }

    private void init() {
        this.paint = new Paint();
        this.paint.setAntiAlias(true);
        this.paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int center = getWidth() / 2;
        float innerCircle = innerCirclePercent * center;

        this.paint.setColor(backgroundColor);

        canvas.drawCircle(center, center, innerCircle, this.paint);
        if (isChecked()) {
            drawCircleBorder(canvas, center, center, (int) (innerCircle + borderWidth / 2), borderWidth, borderColor);
        }
        super.onDraw(canvas);
    }

    private void drawCircleBorder(Canvas canvas, float centerX, float centerY, int radius, float borderWidth, int color) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getInnerCirclePercent() {
        return innerCirclePercent;
    }

    public void setInnerCirclePercent(float innerCirclePercent) {
        this.innerCirclePercent = innerCirclePercent;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public float getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(float borderWidth) {
        this.borderWidth = borderWidth;
    }
}
