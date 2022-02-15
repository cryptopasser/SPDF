package com.snakeway.spdf;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.widget.PopupWindow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/8 17:18
 */
public class BaseActivity<T extends ViewBinding> extends AppCompatActivity {

    public Handler handler = null;
    public T viewBinding = null;
    private boolean allowKeyBack = true;
    private HashMap<String, ProgressDialog> hashMapProgressDialogs = new HashMap<String, ProgressDialog>();
    private HashMap<String, PopupWindow> hashMapPopupWindows = new HashMap<String, PopupWindow>();


    public void addProgressDialog(String progressDialogKey, ProgressDialog progressDialog) {
        if (progressDialogKey != null && progressDialog != null) {
            hashMapProgressDialogs.put(progressDialogKey, progressDialog);
        }
    }

    public ProgressDialog getProgressDialog(String progressDialogKey) {
        if (progressDialogKey == null) {
            return null;
        }
        if (hashMapProgressDialogs.containsKey(progressDialogKey)) {
            return hashMapProgressDialogs.get(progressDialogKey);
        } else {
            return null;
        }
    }

    public void removeProgressDialog(String progressDialogKey) {
        if (progressDialogKey != null) {
            if (hashMapProgressDialogs.containsKey(progressDialogKey)) {
                hashMapProgressDialogs.remove(progressDialogKey);
            }
        }
    }

    public void dismissAndRemoveAllProgressDialog() {
        Iterator<Map.Entry<String, ProgressDialog>> iterator = hashMapProgressDialogs.entrySet().iterator();
        List<ProgressDialog> progressDialogs = new ArrayList<ProgressDialog>();
        while (iterator.hasNext()) {
            Map.Entry<String, ProgressDialog> entry = iterator.next();
            ProgressDialog progressDialog = entry.getValue();
            progressDialogs.add(progressDialog);
        }
        for (int i = 0; i < progressDialogs.size(); i++) {
            progressDialogs.get(i).dismiss();
        }
        hashMapProgressDialogs.clear();
    }

    public void addPopupWindow(String popupWindowKey, PopupWindow popupWindow) {
        if (popupWindowKey != null && popupWindow != null) {
            hashMapPopupWindows.put(popupWindowKey, popupWindow);
        }
    }

    public PopupWindow getPopupWindow(String popupWindowKey) {
        if (popupWindowKey == null) {
            return null;
        }
        if (hashMapPopupWindows.containsKey(popupWindowKey)) {
            return hashMapPopupWindows.get(popupWindowKey);
        } else {
            return null;
        }
    }

    public void removePopupWindow(String popupWindowKey) {
        if (popupWindowKey != null) {
            if (hashMapPopupWindows.containsKey(popupWindowKey)) {
                hashMapPopupWindows.remove(popupWindowKey);
            }
        }
    }

    public void dismissAndRemoveAllPopupWindow() {
        Iterator<Map.Entry<String, PopupWindow>> iterator = hashMapPopupWindows.entrySet().iterator();
        List<PopupWindow> popupWindows = new ArrayList<PopupWindow>();
        while (iterator.hasNext()) {
            Map.Entry<String, PopupWindow> entry = iterator.next();
            PopupWindow popupWindow = entry.getValue();
            popupWindows.add(popupWindow);
        }
        for (int i = 0; i < popupWindows.size(); i++) {
            popupWindows.get(i).dismiss();
        }
        hashMapPopupWindows.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getViewBinder() != null) {
            viewBinding = getViewBinder();
            setContentView(viewBinding.getRoot());
        }
    }

    protected T getViewBinder() {
        return null;
    }

    protected void initAll() {
        initHandler();
        initUi();
        initConfigUi();
        initHttp();
        initOther();
    }

    public void initHandler() {

    }

    public void initUi() {

    }

    public void initConfigUi() {

    }

    public void initHttp() {

    }

    public void initOther() {

    }

    public boolean isAllowKeyBack() {
        return allowKeyBack;
    }

    public void setAllowKeyBack(boolean allowKeyBack) {
        this.allowKeyBack = allowKeyBack;
    }

    public void doBack() {

    }

    public String getTag() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (allowKeyBack) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                doBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        dismissAndRemoveAllProgressDialog();
        dismissAndRemoveAllPopupWindow();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().
                getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics displayMetrics = context.getResources().
                getDisplayMetrics();
        return displayMetrics.heightPixels;
    }

    public static Activity getActivityByContext(Context context) {
        if (context == null) {
            return null;
        }
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getActivityByContext(((ContextWrapper) context).getBaseContext());
        } else {
            return null;
        }
    }
}
