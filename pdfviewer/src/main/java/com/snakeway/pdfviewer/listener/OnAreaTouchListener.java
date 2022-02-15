package com.snakeway.pdfviewer.listener;

import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.snakeway.pdfviewer.annotation.base.MarkAreaType;

import java.util.List;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/12 9:32
 */
public interface OnAreaTouchListener {

    void onActiveArea();

    void onAreaSelect(@NonNull RectF startRect, @NonNull RectF endRect, float translateX, float translateY, float targetViewSize, @NonNull List<MarkAreaType> selectMarkAreaTypes);

    void onReTouchStart();

    void onReTouchAreaSelectUpdate(@NonNull RectF startRect, @NonNull RectF endRect, float translateX, float translateY, float targetViewSize, @NonNull List<MarkAreaType> selectMarkAreaTypes);

    void onReTouchComplete();

    void onDismiss();

}
