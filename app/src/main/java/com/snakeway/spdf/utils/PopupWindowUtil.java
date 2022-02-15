package com.snakeway.spdf.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.spdf.BaseActivity;
import com.snakeway.spdf.R;
import com.snakeway.spdf.adapter.ReadModeAdapter;
import com.snakeway.spdf.models.BaseBookMarkBean;
import com.snakeway.spdf.models.BookMarkBean;
import com.snakeway.spdf.views.StatusView;
import com.snakeway.spdf.views.TreeControlView;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/10 9:14
 */
public class PopupWindowUtil {
    public static final String POPUPWINDOWKEY = "popupWindowKey";

    public interface OnShowPopupWindowOperatingListener {

        boolean onSelect(PopupWindow popupWindow, View view, MarkAreaType markAreaType);

        boolean onCancelSelect(PopupWindow popupWindow, View view, MarkAreaType markAreaType);

        void clearPage(PopupWindow popupWindow, View view);

        void onDismiss(PopupWindow popupWindow, View view);

    }

    public interface OnShowPopupWindowBookMarketListener {

        void onClick(PopupWindow popupWindow, View view);

        void onDismiss(PopupWindow popupWindow, View view);

    }


    public static PopupWindow showPopupWindowOperating(final BaseActivity baseActivity, View view, final View needEnableView, final String key, final int x, final int y, final boolean isUp, final List<MarkAreaType> selectMarkAreaTypes, final OnShowPopupWindowOperatingListener onShowPopupWindowOperatingListener) {
        if (view.getWindowToken() == null) {
            return null;
        }
        if (needEnableView != null) {
            needEnableView.setEnabled(false);
        }
        LayoutInflater layoutInflater = baseActivity.getLayoutInflater();
        View popupWindowView = layoutInflater.inflate(R.layout.popupwindow_operating, null);
        final String popupWindowKey = key != null ? key : (POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep());
        final PopupWindow popupWindow = new PopupWindow(popupWindowView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setPopupWindowTouchModal(popupWindow, false);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
//                windowManagerLayoutParams.alpha = 1.0f;
//                baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
                if (needEnableView != null) {
                    needEnableView.setEnabled(true);
                }
                if (onShowPopupWindowOperatingListener != null) {
                    onShowPopupWindowOperatingListener.onDismiss(popupWindow, popupWindowView);
                }
                baseActivity.removePopupWindow(popupWindowKey);
            }
        });
        final ImageView imageViewTriangleUp = (ImageView) popupWindowView.findViewById(R.id.imageViewTriangleUp);
        final ImageView imageViewTriangleDown = (ImageView) popupWindowView.findViewById(R.id.imageViewTriangleDown);
        final StatusView statusViewDeleteLine = (StatusView) popupWindowView.findViewById(R.id.statusViewDeleteLine);
        final StatusView statusViewUnderLine = (StatusView) popupWindowView.findViewById(R.id.statusViewUnderLine);
        final StatusView statusViewUnderWaveLine = (StatusView) popupWindowView.findViewById(R.id.statusViewUnderWaveLine);
        final StatusView statusViewHighLight = (StatusView) popupWindowView.findViewById(R.id.statusViewHighLight);
        final StatusView statusViewClear = (StatusView) popupWindowView.findViewById(R.id.statusViewClear);

        if (selectMarkAreaTypes != null) {
            for (MarkAreaType markAreaType : selectMarkAreaTypes) {
                switch (markAreaType) {
                    case DELETELINE:
                        statusViewDeleteLine.setChecked(true);
                        break;
                    case UNDERLINE:
                        statusViewUnderLine.setChecked(true);
                        break;
                    case UNDERWAVELINE:
                        statusViewUnderWaveLine.setChecked(true);
                        break;
                    case HIGHLIGHT:
                        statusViewHighLight.setChecked(true);
                        break;
                    default:
                        break;
                }
            }
        }
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StatusView statusView;
                MarkAreaType markAreaType;
                switch (view.getId()) {
                    case R.id.statusViewDeleteLine:
                        statusView = statusViewDeleteLine;
                        markAreaType = MarkAreaType.DELETELINE;
                        break;
                    case R.id.statusViewUnderLine:
                        statusView = statusViewUnderLine;
                        markAreaType = MarkAreaType.UNDERLINE;
                        break;
                    case R.id.statusViewUnderWaveLine:
                        statusView = statusViewUnderWaveLine;
                        markAreaType = MarkAreaType.UNDERWAVELINE;
                        break;
                    case R.id.statusViewHighLight:
                        statusView = statusViewHighLight;
                        markAreaType = MarkAreaType.HIGHLIGHT;
                        break;
                    case R.id.statusViewClear:
                        onShowPopupWindowOperatingListener.clearPage(popupWindow, view);
                        return;
                    default:
                        return;
                }
                boolean isChecked = statusView.isChecked();
                boolean result = false;
                if (isChecked) {
                    result = onShowPopupWindowOperatingListener.onCancelSelect(popupWindow, view, markAreaType);
                } else {
                    result = onShowPopupWindowOperatingListener.onSelect(popupWindow, view, markAreaType);
                }
                if (result) {
                    statusView.setChecked(!isChecked);
                }
            }
        };
        statusViewDeleteLine.setOnClickListener(onClickListener);
        statusViewUnderLine.setOnClickListener(onClickListener);
        statusViewUnderWaveLine.setOnClickListener(onClickListener);
        statusViewHighLight.setOnClickListener(onClickListener);
        statusViewClear.setOnClickListener(onClickListener);
        if (isUp) {
            imageViewTriangleUp.setVisibility(View.VISIBLE);
            imageViewTriangleDown.setVisibility(View.GONE);
        } else {
            imageViewTriangleUp.setVisibility(View.GONE);
            imageViewTriangleDown.setVisibility(View.VISIBLE);
        }
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        popupWindowView.measure(spec, spec);
        int measuredWidth = popupWindowView.getMeasuredWidth();
        int measuredHeight = popupWindowView.getMeasuredHeight();

        ColorDrawable colorDrawable = new ColorDrawable(Color.argb(0, 255, 255, 255));
        popupWindow.setBackgroundDrawable(colorDrawable);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(R.style.popwindowNormalAnimationCenter);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
//        windowManagerLayoutParams.alpha = 0.7f;
//        baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
        if (isUp) {
            popupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP, x - measuredWidth / 2, y);
        } else {
            popupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP, x - measuredWidth / 2, y - measuredHeight);
        }
        baseActivity.addPopupWindow(popupWindowKey, popupWindow);
        return popupWindow;
    }

    public static PopupWindow showPopupWindowBookMarket(final BaseActivity baseActivity, View view, final View needEnableView, final String key, final List<PdfDocument.Bookmark> bookmarks, final BaseBookMarkBean.OnBookMarkListener onBookMarkListener, final OnShowPopupWindowBookMarketListener onShowPopupWindowBookMarketListener) {
        if (view.getWindowToken() == null || bookmarks == null) {
            return null;
        }
        if (needEnableView != null) {
            needEnableView.setEnabled(false);
        }
        LayoutInflater layoutInflater = baseActivity.getLayoutInflater();
        View popupWindowView = layoutInflater.inflate(R.layout.popupwindow_book_market, null);
        final String popupWindowKey = key != null ? key : (POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep());
        int popupWindowWidth = BaseActivity.getScreenWidth(baseActivity) * 3 / 5;
        final PopupWindow popupWindow = new PopupWindow(popupWindowView, popupWindowWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
                windowManagerLayoutParams.alpha = 1.0f;
                baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
                if (needEnableView != null) {
                    needEnableView.setEnabled(true);
                }
                if (onShowPopupWindowBookMarketListener != null) {
                    onShowPopupWindowBookMarketListener.onDismiss(popupWindow, popupWindowView);
                }
                baseActivity.removePopupWindow(popupWindowKey);
            }
        });
        final TreeControlView treeControlView = (TreeControlView) popupWindowView.findViewById(R.id.treeControlView);
        treeControlView.refreshAllItem(BookMarkBean.convertBookMark(bookmarks, onBookMarkListener), true);

        ColorDrawable colorDrawable = new ColorDrawable(Color.argb(0, 255, 255, 255));
        popupWindow.setBackgroundDrawable(colorDrawable);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(R.style.popwindowNormalAnimationLeft);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
        windowManagerLayoutParams.alpha = 0.7f;
        baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
        popupWindow.showAtLocation(view, Gravity.LEFT, 0, 0);
        baseActivity.addPopupWindow(popupWindowKey, popupWindow);
        return popupWindow;
    }


    public interface OnShowPopupWindowReadModeListener {

        void onSelect(int position, ReadModeAdapter readModeAdapter, PopupWindow popupWindow, View view);

        void onDismiss(PopupWindow popupWindow, View view);

    }

    public static PopupWindow showPopupWindowReadMode(final BaseActivity baseActivity, View view, final View needEnableView, final String key, final ReadModeAdapter readModeAdapter, final OnShowPopupWindowReadModeListener onShowPopupWindowReadModeListener) {
        if (view.getWindowToken() == null) {
            return null;
        }
        if (needEnableView != null) {
            needEnableView.setEnabled(false);
        }

        LayoutInflater layoutInflater = baseActivity.getLayoutInflater();
        View popupWindowView = layoutInflater.inflate(R.layout.popupwindow_read_mode, null);
        final String popupWindowKey = key != null ? key : (POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep());
        int popupWindowHeight = (int) (BaseActivity.getScreenHeight(baseActivity) * 0.56f);
        final PopupWindow popupWindow = new PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT, popupWindowHeight);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
                windowManagerLayoutParams.alpha = 1.0f;
                baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
                if (needEnableView != null) {
                    needEnableView.setEnabled(true);
                }
                if (onShowPopupWindowReadModeListener != null) {
                    onShowPopupWindowReadModeListener.onDismiss(popupWindow, popupWindowView);
                }
                baseActivity.removePopupWindow(popupWindowKey);
            }
        });
        final ImageView imageViewClose = (ImageView) popupWindowView.findViewById(R.id.imageViewClose);
        final ListView listView = (ListView) popupWindowView.findViewById(R.id.listView);

        listView.setAdapter(readModeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (onShowPopupWindowReadModeListener != null) {
                    onShowPopupWindowReadModeListener.onSelect(position, readModeAdapter, popupWindow, view);
                }
            }
        });

        imageViewClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        ColorDrawable colorDrawable = new ColorDrawable(Color.argb(0, 255, 255, 255));
        popupWindow.setBackgroundDrawable(colorDrawable);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setAnimationStyle(R.style.popwindowNormalAnimation);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowManager.LayoutParams windowManagerLayoutParams = baseActivity.getWindow().getAttributes();
        windowManagerLayoutParams.alpha = 0.7f;
        baseActivity.getWindow().setAttributes(windowManagerLayoutParams);
        popupWindow.showAtLocation(view, Gravity.BOTTOM, 0, 0);
        baseActivity.addPopupWindow(popupWindowKey, popupWindow);
        return popupWindow;
    }


    public static void setPopupWindowTouchModal(PopupWindow popupWindow,
                                                boolean touchModal) {
        if (null == popupWindow) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popupWindow.setTouchModal(touchModal);
            return;
        }
        Method method;
        try {
            method = PopupWindow.class.getDeclaredMethod("setTouchModal",
                    boolean.class);
            method.setAccessible(true);
            method.invoke(popupWindow, touchModal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
