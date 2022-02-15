package com.snakeway.pdfviewer.util;

import android.text.Selection;
import android.text.Spannable;
import android.widget.EditText;

/**
 * @author snakeway
 * @description:
 * @date :2021/8/2 10:31
 */
public class EditTextUtil {

    public static void setCursorToLast(EditText editText) {
        CharSequence charSequence = editText.getText();
        if (charSequence != null && charSequence instanceof Spannable) {
            Spannable spannable = (Spannable) charSequence;
            Selection.setSelection(spannable, spannable.length());
        }
    }

}
