package com.snakeway.pdfviewer.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author snakeway
 * @description:
 * @date :2022/3/31 14:38
 */
public class Base64Util {

    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static String bitmapToBase64(Bitmap bitmap, boolean isPng, double targetWidth) {
        String result = null;
        if (bitmap == null) {
            return null;
        }
        if(targetWidth>0) {
            float width = bitmap.getWidth();
            float height = bitmap.getHeight();
            Matrix matrix = new Matrix();
            float scaleWidth = targetWidth > 0 ? ((float) targetWidth) / width : 1;
            matrix.postScale(scaleWidth, scaleWidth);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) width,
                    (int) height, matrix, true);
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bitmap.compress(isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 100, baos);
            baos.flush();
            baos.close();
            byte[] bitmapBytes = baos.toByteArray();
            result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
