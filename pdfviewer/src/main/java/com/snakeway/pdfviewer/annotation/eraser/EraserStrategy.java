package com.snakeway.pdfviewer.annotation.eraser;

import android.graphics.Rect;

import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

/**
 * @author snakeway
 */
public interface EraserStrategy {

    boolean erase(PDFView view, BaseAnnotation annotation, Rect rect);
}
