package com.snakeway.fileviewer.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/8 15:35
 */
public class ToastUtil {

    public static void showShortToast(Context context, String msg) {
        if (context == null) return;
        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showShortToast(Context context, int msgResId) {
        if (context == null) return;
        Toast.makeText(context.getApplicationContext(), context.getResources().getString(msgResId), Toast.LENGTH_SHORT).show();
    }

//    public static void showLongToast(Context context, String msg) {
//        if (context == null) return;
//        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
//    }
//
//    public static void showLongToast(Context context, int msgResId) {
//        if (context == null) return;
//        Toast.makeText(context.getApplicationContext(), context.getResources().getString(msgResId), Toast.LENGTH_LONG).show();
//    }
}
