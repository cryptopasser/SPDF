package com.snakeway.pdfviewer.annotation;

import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

/**
 * @author snakeway
 */
public interface AnnotationListener {

    void onAnnotationAdd(BaseAnnotation annotation);

    void onAnnotationRemove(BaseAnnotation annotation);

    void onAnnotationPageRemove(int page);

    void onAnnotationAllRemove();

}
