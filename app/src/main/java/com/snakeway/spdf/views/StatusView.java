package com.snakeway.spdf.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.snakeway.spdf.R;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/9 17:11
 */
public class StatusView extends LinearLayout {
    Context context;

    boolean checked;
    boolean clickChange;

    int checkImage;
    int unCheckImage;
    int checkImageColor;
    int unCheckImageColor;
    int checkTextColor;
    int unCheckTextColor;
    float textSize;
    String checkText;
    String unCheckText;

    ImageView background;
    TextView textView;

    public StatusView(Context context) {
        this(context, null, 0);
    }

    public StatusView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StatusView, defStyleAttr, 0);
        checked = typedArray.getBoolean(R.styleable.StatusView_checked, false);
        clickChange = typedArray.getBoolean(R.styleable.StatusView_clickChange, false);
        checkImage = typedArray.getResourceId(R.styleable.StatusView_checkImage, 0);
        unCheckImage = typedArray.getResourceId(R.styleable.StatusView_unCheckImage, 0);
        checkImageColor = typedArray.getColor(R.styleable.StatusView_checkImageColor, 0);
        unCheckImageColor = typedArray.getColor(R.styleable.StatusView_unCheckImageColor, 0);
        checkTextColor = typedArray.getColor(R.styleable.StatusView_checkTextColor, Color.BLACK);
        unCheckTextColor = typedArray.getColor(R.styleable.StatusView_unCheckTextColor, Color.BLACK);
        textSize = typedArray.getDimension(R.styleable.StatusView_textSize, 2);
        checkText = typedArray.getString(R.styleable.StatusView_checkText);
        unCheckText = typedArray.getString(R.styleable.StatusView_unCheckText);
        initUi();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (clickChange) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                checked = !checked;
                updateStatus();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    void initUi() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        View view = inflate(context, R.layout.view_status, this);
        background = view.findViewById(R.id.statusViewImage);
        textView = new TextView(context);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);//TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(textView, 6, 24, 1, TypedValue.COMPLEX_UNIT_SP);
        addView(textView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        updateStatus();
    }

    void updateStatus() {
        if ((checked && checkImageColor != 0) || (!checked && unCheckImageColor != 0)) {
            Drawable originalDrawable = ContextCompat.getDrawable(getContext(), checked ? checkImage : unCheckImage);
            Drawable tintDrawable = DrawableCompat.wrap(originalDrawable).mutate();
            DrawableCompat.setTint(tintDrawable, checked ? checkImageColor : unCheckImageColor);
            background.setImageDrawable(tintDrawable);
        } else {
            background.setImageResource(checked ? checkImage : unCheckImage);
        }
        textView.setTextColor(checked ? checkTextColor : unCheckTextColor);
        textView.setText(checked ? checkText : unCheckText);
    }

    public TextView getText() {
        return textView;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        updateStatus();
    }

    public boolean isClickChange() {
        return clickChange;
    }

    public void setClickChange(boolean clickChange) {
        this.clickChange = clickChange;
    }

    public int getCheckImage() {
        return checkImage;
    }

    public void setCheckImage(int checkImage) {
        this.checkImage = checkImage;
        updateStatus();
    }

    public int getUnCheckImage() {
        return unCheckImage;
    }

    public void setUnCheckImage(int unCheckImage) {
        this.unCheckImage = unCheckImage;
        updateStatus();
    }

    public int getCheckTextColor() {
        return checkTextColor;
    }

    public void setCheckTextColor(int checkTextColor) {
        this.checkTextColor = checkTextColor;
        updateStatus();
    }

    public int getUnCheckTextColor() {
        return unCheckTextColor;
    }

    public void setUnCheckTextColor(int unCheckTextColor) {
        this.unCheckTextColor = unCheckTextColor;
        updateStatus();
    }
}
