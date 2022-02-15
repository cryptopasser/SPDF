package com.snakeway.pdfviewer.annotation;

import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;

/**
 * @author snakeway
 */
public interface AnnotionListener {

    void onAnnotionAdd(BaseAnnotation annotation);

    void onAnnotionRemove(BaseAnnotation annotation);

    void onAnnotionPageRemove(int page);

    void onAnnotionAllRemove();

}
