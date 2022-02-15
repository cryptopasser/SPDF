package com.snakeway.pdfviewer.listener;

import android.widget.EditText;

import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.model.TextRemarkInfo;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/12 9:32
 */
public interface OnTextRemarkListener {

    void onShow(EditText editText);

    void onSave(EditText editText, TextRemarkInfo textRemarkInfo);

    void onDelete(EditText editText, TextAnnotation textAnnotation);

    void onCancel(EditText editText, boolean isEdit);

}
