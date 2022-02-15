package com.snakeway.pdfviewer.util;

import android.graphics.Bitmap;
import android.view.View;

/**
 * @author snakeway
 * @description:
 * @date :2021/7/28 16:43
 */
public class BitmapUtil {

    public static Bitmap getViewBitmap(View view) {
        Bitmap bitmap = null;
        try {
            view.setDrawingCacheEnabled(true);
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0,
                    view.getMeasuredWidth(),
                    view.getMeasuredHeight());
            view.buildDrawingCache();
            Bitmap cacheBitmap = view.getDrawingCache();
            bitmap = Bitmap.createBitmap(cacheBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
