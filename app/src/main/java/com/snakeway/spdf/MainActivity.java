package com.snakeway.spdf;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.CacheDiskUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.google.gson.reflect.TypeToken;
import com.snakeway.floatball.menu.MenuItem;
import com.snakeway.floatball.utils.BackGroudSeletor;
import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.annotation.AnnotationBean;
import com.snakeway.pdfviewer.annotation.AnnotationListener;
import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.pdfviewer.annotation.pen.AreaPen;
import com.snakeway.pdfviewer.annotation.pen.DeleteLinePen;
import com.snakeway.pdfviewer.annotation.pen.HighLightPen;
import com.snakeway.pdfviewer.annotation.pen.Pen;
import com.snakeway.pdfviewer.annotation.pen.PenBuilder;
import com.snakeway.pdfviewer.annotation.pen.TextPen;
import com.snakeway.pdfviewer.annotation.pen.UnderLinePen;
import com.snakeway.pdfviewer.annotation.pen.UnderWaveLinePen;
import com.snakeway.pdfviewer.listener.OnAreaTouchListener;
import com.snakeway.pdfviewer.listener.OnLoadCompleteListener;
import com.snakeway.pdfviewer.listener.OnPageChangeListener;
import com.snakeway.pdfviewer.listener.OnPageErrorListener;
import com.snakeway.pdfviewer.listener.OnSearchTextListener;
import com.snakeway.pdfviewer.listener.OnTextRemarkListener;
import com.snakeway.pdfviewer.model.SearchTextInfo;
import com.snakeway.pdfviewer.model.TextRemarkInfo;
import com.snakeway.pdfviewer.util.FitPolicy;
import com.snakeway.spdf.adapter.ReadModeAdapter;
import com.snakeway.spdf.databinding.ActivityMainBinding;
import com.snakeway.spdf.models.BaseBookMarkBean;
import com.snakeway.spdf.models.ReadModeItem;
import com.snakeway.spdf.utils.AnimationUtil;
import com.snakeway.spdf.utils.PopupWindowUtil;
import com.snakeway.spdf.utils.ScreenTool;
import com.snakeway.spdf.utils.TimeUtil;
import com.snakeway.spdf.views.CircleView;
import com.snakeway.spdf.views.StatusView;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

import static com.snakeway.spdf.ThumbnailActivity.RESULT_PAGE_KEY;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/4 14:18
 */
public class MainActivity extends BaseActivity<ActivityMainBinding> implements OnPageChangeListener, OnLoadCompleteListener,
        OnPageErrorListener {
    public static final int THUMBNAIL_CHOOSE_REQUEST = 2222;
    public static final String FILE_PATH_KEY = "file_path_key";
    public static final String FILE_PASSWORD_KEY = "file_password_key";

    private final String PDF_NAME = "test.pdf";
    private final String PDF_PASSWORD = "123456";

    public static final String SAVE_ANNOTATION_KEY = "save_annotation_key";


    private View.OnClickListener onClickListener;

    private PDFView.Configurator configurator;
    private Integer pageNumber = 0;
    private String popupWindowOperatingKey;
    private String popupWindowBookMarketKey;

    private Pen.WritePen pen;
    private int selectPenIndex = 0;

    private TextPen textPen;
    private int selectTextPenIndex = 0;

    private boolean isActiveSearch = false;

    private String searchContent;

    private List<String> annotations = new ArrayList<>();

    final List<ReadModeItem> datas = new ArrayList<>();
    private ReadModeAdapter adapter;

    private boolean isTextRemark = false;

    final List<CircleView> circleViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAll();
    }

    @Override
    protected ActivityMainBinding getViewBinder() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    public void initHandler() {
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void initUi() {
        initPenColorViews();
        onClickListener();
        onSearchListener();
        addSearchKeyListener();
        addSearchObserver();
    }

    @Override
    public void initConfigUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int statusBarHeight = ScreenTool.getStatusBarHeight(MainActivity.this);
            ViewGroup.LayoutParams layoutParams = viewBinding.layoutSearchAppbar.toolbar.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = layoutParams.height + statusBarHeight;
            }
            RelativeLayout.MarginLayoutParams relativeLayoutToolbarLayoutParams = (RelativeLayout.MarginLayoutParams) viewBinding.layoutSearchAppbar.relativeLayoutToolbar.getLayoutParams();
            if (relativeLayoutToolbarLayoutParams != null) {
                relativeLayoutToolbarLayoutParams.topMargin = statusBarHeight;
            }
        }

        initFloatBallControl();

        viewBinding.layoutSearch.relativeLayoutSearch.setVisibility(View.GONE);

        openPdf(PDF_NAME, PDF_PASSWORD);
        viewBinding.pdfView.setMinZoom(1F);
        viewBinding.pdfView.setMidZoom(2F);
        viewBinding.pdfView.setMaxZoom(3F);

        AreaPen areaPen = PenBuilder.areaPenBuilder().setColor(getResources().getColor(R.color.areaPen_default)).build();
        areaPen.setCursorColor(getResources().getColor(R.color.areaPen_cursor_default));

        DeleteLinePen deleteLinePen = PenBuilder.deleteLinePenBuilder().setColor(getResources().getColor(R.color.deleteLinePen_default)).build();
        UnderLinePen underLinePen = PenBuilder.underLinePenBuilder().setColor(getResources().getColor(R.color.underLinePen_default)).build();
        UnderWaveLinePen underWaveLinePen = PenBuilder.underWaveLinePenBuilder().setColor(getResources().getColor(R.color.wavesLinePen_default)).build();
        HighLightPen highLightPen = PenBuilder.selectedPenBuilder().setColor(getResources().getColor(R.color.highLightPen_default)).build();
        viewBinding.pdfView.setAreaPen(areaPen);
        viewBinding.pdfView.setDrawAreaPen(MarkAreaType.DELETELINE, deleteLinePen);
        viewBinding.pdfView.setDrawAreaPen(MarkAreaType.UNDERLINE, underLinePen);
        viewBinding.pdfView.setDrawAreaPen(MarkAreaType.UNDERWAVELINE, underWaveLinePen);
        viewBinding.pdfView.setDrawAreaPen(MarkAreaType.HIGHLIGHT, highLightPen);
        viewBinding.pdfView.setSearchAreaPen(PenBuilder.searchAreaPenBuilder().setColor(getResources().getColor(R.color.searchAreaPen_default)).build());
        viewBinding.pdfView.addAnnotations(getAnnotationData(), false);
        viewBinding.pdfView.setProgressViewBackground(getResources().getColor(R.color.pdf_view_background));
        viewBinding.pdfView.setAnnotationListener(new AnnotationListener() {
            @Override
            public void onAnnotationAdd(BaseAnnotation penAnnotation) {
                String result = GsonUtils.toJson(viewBinding.pdfView.getOptimizationAnnotation(penAnnotation));
                annotations.add(result);
                saveAnnotationData();
            }

            @Override
            public void onAnnotationRemove(BaseAnnotation penAnnotation) {
                saveAnnotationData();
            }

            @Override
            public void onAnnotationPageRemove(int page) {
                saveAnnotationData();
            }

            @Override
            public void onAnnotationAllRemove() {
                saveAnnotationData();
            }
        });
        viewBinding.pdfView.setOnAreaTouchListener(new OnAreaTouchListener() {
            @Override
            public void onActiveArea() {
                doVibrate();
            }

            @Override
            public void onAreaSelect(@NonNull RectF startRect, @NonNull RectF endRect, float translateX, float translateY, float targetViewSize, List<MarkAreaType> selectMarkAreaTypes) {
                int[] position = getPopupWindowShowPosition(startRect, endRect, translateX, translateY, targetViewSize);
                showPopupWindowOperating(viewBinding.rootView, position[0], position[1], position[2] == 1, selectMarkAreaTypes);
            }

            @Override
            public void onReTouchStart() {
                visiblePopupWindowOperating(false);
            }

            @Override
            public void onReTouchAreaSelectUpdate(@NonNull RectF startRect, @NonNull RectF endRect, float translateX, float translateY, float targetViewSize, @NonNull List<MarkAreaType> selectMarkAreaTypes) {
                updatePopupWindowPosition(startRect, endRect, translateX, translateY, targetViewSize, selectMarkAreaTypes);
            }

            @Override
            public void onReTouchComplete() {
                visiblePopupWindowOperating(true);
            }

            @Override
            public void onDismiss() {
                dismissPopupWindowOperating();
            }
        });
        viewBinding.pdfView.setOnTextRemarkListener(new OnTextRemarkListener() {
            @Override
            public void onShow(EditText editText) {
                ScreenTool.showSoftInput(MainActivity.this, true, editText);
            }

            @Override
            public void onSave(EditText editText, TextRemarkInfo textRemarkInfo) {
                ScreenTool.showSoftInput(MainActivity.this, false, editText);
                showRemarkView(false, true);
            }

            @Override
            public void onDelete(EditText editText, TextAnnotation textAnnotation) {
                ScreenTool.showSoftInput(MainActivity.this, false, editText);
            }

            @Override
            public void onCancel(EditText editText, boolean isEdit) {
                ScreenTool.showSoftInput(MainActivity.this, false, editText);
            }
        });
        hideBottomUIMenu();
        viewBinding.pdfView.post(new Runnable() {
            @Override
            public void run() {
                viewBinding.pdfView.setTextPen(textPen);
            }
        });

    }

    @Override
    public void initHttp() {
    }

    @Override
    public void initOther() {
        requestPermissions();
    }

    private void initFloatBallControl() {
        List<MenuItem> menuItems = new ArrayList<>();
        MenuItem menuItemEmpty = new MenuItem(null) {
            @Override
            public void action() {
                viewBinding.floatBallControlView.getFloatBallManager().closeMenu();
            }
        };
        menuItems.add(menuItemEmpty);
        menuItems.add(new MenuItem(BackGroudSeletor.getdrawble("icon_menu_1", this)) {
            @Override
            public void action() {
                showBookmarks();
                viewBinding.floatBallControlView.getFloatBallManager().closeMenu();
            }
        });
        menuItems.add(new MenuItem(BackGroudSeletor.getdrawble("icon_menu_2", this)) {
            @Override
            public void action() {
                showReadMode();
                viewBinding.floatBallControlView.getFloatBallManager().closeMenu();
            }
        });
        menuItems.add(new MenuItem(BackGroudSeletor.getdrawble("icon_menu_3", this)) {
            @Override
            public void action() {
                viewBinding.floatBallControlView.getFloatBallManager().closeMenu();
            }
        });
        menuItems.add(menuItemEmpty);
        viewBinding.floatBallControlView.init(menuItems);
    }

    private void hideBottomUIMenu() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            WindowManager.LayoutParams params = window.getAttributes();
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            window.setAttributes(params);
        }
    }

    private void onClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.imageViewBack:
                        if (annotations.size() > 0) {
                            addRemoveAnnotation(annotations.get(0), false);
                            annotations.remove(0);
                        }
                        break;
                    case R.id.imageViewSearch:
                        showSearchView(true);
                        break;
                    case R.id.textViewSearchCancel:
                        viewBinding.pdfView.clearSearchArea();
                        showSearchView(false);
                        break;
                    case R.id.buttonOpenOther:
                        FileViewerActivity.openFileViewerActivity(MainActivity.this);
                        break;
                    case R.id.frameLayoutRemark:
                        dismissPopupWindowOperating();
                        showRemarkView(true, false);
                        break;
                    case R.id.frameLayoutTextRemark:
                        dismissPopupWindowOperating();
                        showRemarkView(true, true);
                        break;
                    case R.id.textViewCancel:
                        if (!isTextRemark) {
                            cancelPenDrawing();
                        } else {
                            showRemarkView(false, true);
                        }
                        break;
                    case R.id.textViewSave:
                        if (!isTextRemark) {
                            savePenDrawing();
                        } else {
                            showRemarkView(false, true);
                        }
                        break;
                    case R.id.imageViewSearchClear:
                        viewBinding.layoutSearch.editTextSearch.setText("");
                        viewBinding.pdfView.clearSearchArea();
                        break;
                    case R.id.textViewSearch:
                        searchText(searchContent);
                        break;
                    case R.id.frameLayoutCover:
                        showSearchView(false);
                        break;
                    default:
                        break;
                }
            }
        };
        viewBinding.layoutSearchAppbar.imageViewBack.setOnClickListener(onClickListener);
        viewBinding.layoutSearchAppbar.imageViewSearch.setOnClickListener(onClickListener);
        viewBinding.layoutSearchAppbar.textViewSearchCancel.setOnClickListener(onClickListener);
        viewBinding.buttonOpenOther.setOnClickListener(onClickListener);
        viewBinding.frameLayoutRemark.setOnClickListener(onClickListener);
        viewBinding.frameLayoutTextRemark.setOnClickListener(onClickListener);
        viewBinding.includeLayoutPenOperating.textViewCancel.setOnClickListener(onClickListener);
        viewBinding.includeLayoutPenOperating.textViewSave.setOnClickListener(onClickListener);
        viewBinding.layoutSearch.frameLayoutCover.setOnClickListener(onClickListener);
        viewBinding.layoutSearch.imageViewSearchClear.setOnClickListener(onClickListener);
        viewBinding.layoutSearch.textViewSearch.setOnClickListener(onClickListener);
        viewBinding.pdfView.setOnPdfViewClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("setOnPdfView", "annotation");
            }
        });
        viewBinding.buttonOpenOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap data=viewBinding.pdfView.getRenderingBitmap(0,520);
                viewBinding.imageViewPreview.setImageBitmap(data);
            }
        });
    }

    private void addSearchKeyListener() {
        viewBinding.layoutSearch.editTextSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    searchText(searchContent);
                    return true;
                }
                return false;
            }
        });
    }

    private void addSearchObserver() {
        viewBinding.rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect rect = new Rect();
                viewBinding.rootView.getWindowVisibleDisplayFrame(rect);
                int screenHeight = viewBinding.rootView.getRootView().getHeight();
                int softHeight = screenHeight - rect.bottom;
                if (softHeight > screenHeight / 5) {
                    viewBinding.layoutSearch.relativeLayoutSearch.scrollTo(0, softHeight);
                } else {
                    viewBinding.layoutSearch.relativeLayoutSearch.scrollTo(0, 0);
                }
            }
        });
    }

    private void onSearchListener() {
        viewBinding.layoutSearch.editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchContent = viewBinding.layoutSearch.editTextSearch.getText().toString();
                //  searchText(searchContent);
            }
        });
    }

    private void showBookmarks() {
        if (popupWindowBookMarketKey != null) {
            return;
        }
        List<PdfDocument.Bookmark> bookmarks = viewBinding.pdfView.getTableOfContents();
        popupWindowBookMarketKey = PopupWindowUtil.POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep();
        PopupWindowUtil.showPopupWindowBookMarket(this, viewBinding.rootView, null, popupWindowBookMarketKey, bookmarks, new BaseBookMarkBean.OnBookMarkListener() {
            @Override
            public void onItemClick(BaseBookMarkBean baseBookMarkBean) {
                jumpToPageWithAutoFillCheck((int) baseBookMarkBean.pageIndex);
            }
        }, new PopupWindowUtil.OnShowPopupWindowBookMarketListener() {
            @Override
            public void onClick(PopupWindow popupWindow, View view) {

            }

            @Override
            public void onDismiss(PopupWindow popupWindow, View view) {
                dismissPopupWindowBookMarket();
            }
        });
    }

    private void showReadMode() {
        if (adapter == null) {
            adapter = new ReadModeAdapter(MainActivity.this, datas);
            datas.add(new ReadModeItem(ReadModeItem.ReadModeType.SINGLE, R.mipmap.read_mode_single, getResources().getString(R.string.popupwindow_read_mode_item_single), true));
            datas.add(new ReadModeItem(ReadModeItem.ReadModeType.MULTI, R.mipmap.read_mode_multi, getResources().getString(R.string.popupwindow_read_mode_item_multi), false));
            datas.add(new ReadModeItem(ReadModeItem.ReadModeType.THUMBNAIL, R.mipmap.read_mode_thumbnail, getResources().getString(R.string.popupwindow_read_mode_item_thumbnail), false));
            datas.add(new ReadModeItem(ReadModeItem.ReadModeType.NIGHT, R.mipmap.read_mode_night, getResources().getString(R.string.popupwindow_read_mode_item_night), false));
            datas.add(new ReadModeItem(ReadModeItem.ReadModeType.AUTO_FILL_WHITE_SPACE, R.mipmap.read_mode_single, getResources().getString(R.string.popupwindow_read_mode_item_auto_fill_white_space), true));
        }
        String popupWindowReadModeKey = PopupWindowUtil.POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep();
        PopupWindowUtil.showPopupWindowReadMode(this, viewBinding.rootView, null, popupWindowReadModeKey, adapter, new PopupWindowUtil.OnShowPopupWindowReadModeListener() {
            @Override
            public void onSelect(int position, ReadModeAdapter readModeAdapter, PopupWindow popupWindow, View view) {
                if (position > datas.size() - 1) {
                    return;
                }
                ReadModeItem readModeItem = datas.get(position);
                if (readModeItem.isCheck() && (readModeItem.getType() == ReadModeItem.ReadModeType.SINGLE || readModeItem.getType() == ReadModeItem.ReadModeType.MULTI)) {
                    return;
                }
                configurator.defaultPage(pageNumber);
                if (readModeItem.getType() == ReadModeItem.ReadModeType.SINGLE) {
                    getReadModeItem(ReadModeItem.ReadModeType.MULTI, datas).setCheck(false);
                    getReadModeItem(ReadModeItem.ReadModeType.AUTO_FILL_WHITE_SPACE, datas).setShow(true);
                    ReadModeItem autoFillWhiteSpaceReadModeItem = getReadModeItem(ReadModeItem.ReadModeType.AUTO_FILL_WHITE_SPACE, datas);
                    configurator.swipeHorizontal(true).setAutoFillWhiteSpace(autoFillWhiteSpaceReadModeItem.isCheck()).load();
                    readModeItem.setCheck(true);
                } else if (readModeItem.getType() == ReadModeItem.ReadModeType.MULTI) {
                    getReadModeItem(ReadModeItem.ReadModeType.SINGLE, datas).setCheck(false);
                    getReadModeItem(ReadModeItem.ReadModeType.AUTO_FILL_WHITE_SPACE, datas).setShow(false);
                    configurator.swipeHorizontal(false).setAutoFillWhiteSpace(false).load();
                    readModeItem.setCheck(true);
                } else if (readModeItem.getType() == ReadModeItem.ReadModeType.NIGHT) {
                    if (readModeItem.isCheck()) {
                        configurator.nightMode(false).load();
                        readModeItem.setCheck(false);
                    } else {
                        configurator.nightMode(true).load();
                        readModeItem.setCheck(true);
                    }
                } else if (readModeItem.getType() == ReadModeItem.ReadModeType.AUTO_FILL_WHITE_SPACE) {
                    if (readModeItem.isCheck()) {//之前已经选中过了
                        configurator.setAutoFillWhiteSpace(false).load();
                        readModeItem.setCheck(false);
                    } else {
                        configurator.setAutoFillWhiteSpace(true).load();
                        readModeItem.setCheck(true);
                    }
                } else if (readModeItem.getType() == ReadModeItem.ReadModeType.THUMBNAIL) {
                    ThumbnailActivity.openThumbnailActivity(MainActivity.this, PDF_NAME, PDF_PASSWORD, THUMBNAIL_CHOOSE_REQUEST);
                    popupWindow.dismiss();
                }
                readModeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDismiss(PopupWindow popupWindow, View view) {

            }
        });
    }

    private ReadModeItem getReadModeItem(ReadModeItem.ReadModeType readModeType, List<ReadModeItem> allReadModeItems) {
        for (ReadModeItem readModeItem : allReadModeItems) {
            if (readModeItem.getType() == readModeType) {
                return readModeItem;
            }
        }
        return null;
    }


    private void showSearchView(boolean isShow) {
        if (isShow) {
            isActiveSearch = true;
            viewBinding.layoutSearchAppbar.imageViewSearch.setVisibility(View.GONE);
            viewBinding.layoutSearchAppbar.textViewSearchCancel.setVisibility(View.VISIBLE);
            viewBinding.layoutSearch.relativeLayoutSearch.setVisibility(View.VISIBLE);
            ScreenTool.showSoftInput(MainActivity.this, true, viewBinding.layoutSearch.editTextSearch);
        } else {
            isActiveSearch = false;
            viewBinding.layoutSearchAppbar.imageViewSearch.setVisibility(View.VISIBLE);
            viewBinding.layoutSearchAppbar.textViewSearchCancel.setVisibility(View.GONE);
            viewBinding.layoutSearch.relativeLayoutSearch.setVisibility(View.GONE);
            ScreenTool.showSoftInput(MainActivity.this, false, viewBinding.layoutSearch.editTextSearch);
        }
    }

    private void searchText(String text) {
        if (text == null || text.equals("")) {
            viewBinding.pdfView.clearSearchArea();
            return;
        }
        viewBinding.pdfView.searchText(text, 3, new OnSearchTextListener() {

            @Override
            public void onResult(SearchTextInfo searchTextInfo) {
                if (searchTextInfo == null) {
                    return;
                }
                if (pageNumber != searchTextInfo.getPage()) {
                    viewBinding.pdfView.jumpTo(searchTextInfo.getPage());
                }
                viewBinding.pdfView.drawSearchArea(searchTextInfo);
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void initPenColorViews() {
        int itemWidth = (int) ((getScreenWidth(this) - getResources().getDimension(R.dimen.layout_pen_operating_right_view_width) - 2 * getResources().getDimension(R.dimen.view_normal_margin_narrow)) / 7);
        int operatingHeight = (int) (getResources().getDimension(R.dimen.layout_pen_operating_height) * 0.8);
        if (operatingHeight < itemWidth) {
            itemWidth = operatingHeight;
        }
        List<Integer> colors = new ArrayList<>();
        colors.add(getResources().getColor(R.color.pen_color_1));
        colors.add(getResources().getColor(R.color.pen_color_2));
        colors.add(getResources().getColor(R.color.pen_color_3));
        colors.add(getResources().getColor(R.color.pen_color_4));
        colors.add(getResources().getColor(R.color.pen_color_5));
        colors.add(getResources().getColor(R.color.pen_color_6));
        colors.add(getResources().getColor(R.color.pen_color_7));
        circleViews.clear();
        int borderColor = getResources().getColor(R.color.circleViewBorder_default);
        for (int i = 0; i < colors.size(); i++) {
            CircleView circleView = new CircleView(this);
            circleView.setBackgroundColor(colors.get(i));
            circleView.setInnerCirclePercent(0.6f);
            circleView.setBorderColor(borderColor);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemWidth, itemWidth);
            final int index = i;
            circleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updatePenColor(circleViews, index, isTextRemark);
                }
            });
            viewBinding.includeLayoutPenOperating.linearLayoutPenColor.addView(circleView, layoutParams);
            if (i == 0) {
                circleView.setChecked(true);
            }
            circleViews.add(circleView);
        }
        pen = PenBuilder.colorPenBuilder().setColor(getResources().getColor(R.color.pen_color_1)).setPenWidthScale(0.5f).build();
        textPen = PenBuilder.textPenBuilder().setColor(getResources().getColor(R.color.pen_color_1)).setFontSize(10).build();
    }

    private void updatePenColor(List<CircleView> circleViews, int index, boolean isTextRemark) {
        if (circleViews == null || index > circleViews.size() - 1 || index < 0) {
            return;
        }
        for (int i = 0; i < circleViews.size(); i++) {
            CircleView circleView = circleViews.get(i);
            if (i == index) {
                circleView.setChecked(true);
            } else {
                circleView.setChecked(false);
            }
        }
        CircleView circleView = circleViews.get(index);
        int color = circleView.getBackgroundColor();
        if (!isTextRemark) {
            pen = PenBuilder.colorPenBuilder().setColor(color).setPenWidthScale(0.5f).build();
            viewBinding.pdfView.setPenMode(pen);
            selectPenIndex = index;
        } else {
            textPen = PenBuilder.textPenBuilder().setColor(color).setFontSize(10).build();
            viewBinding.pdfView.setTextMode(textPen);
            selectTextPenIndex = index;
        }
    }

    private void showRemarkView(boolean isShow, boolean isTextRemark) {
        if (isShow) {
            viewBinding.frameLayoutRemark.setVisibility(View.GONE);
            viewBinding.frameLayoutTextRemark.setVisibility(View.GONE);
            viewBinding.frameLayoutPenOperating.setVisibility(View.VISIBLE);
            viewBinding.frameLayoutPenOperating.setAnimation(AnimationUtil.moveToViewLocation(null));
            if (isTextRemark) {
                viewBinding.includeLayoutPenOperating.textViewSave.setVisibility(View.GONE);
                updatePenColor(circleViews, selectTextPenIndex == -1 ? 0 : selectTextPenIndex, true);
            } else {
                viewBinding.includeLayoutPenOperating.textViewSave.setVisibility(View.VISIBLE);
                updatePenColor(circleViews, selectPenIndex == -1 ? 0 : selectPenIndex, false);
            }
            this.isTextRemark = isTextRemark;
        } else {
            viewBinding.frameLayoutRemark.setVisibility(View.VISIBLE);
            viewBinding.frameLayoutTextRemark.setVisibility(View.VISIBLE);
            viewBinding.frameLayoutPenOperating.setAnimation(AnimationUtil.moveToViewBottom(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    viewBinding.frameLayoutPenOperating.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            }));
            viewBinding.pdfView.setViewerMode();
        }
    }


    private void savePenDrawing() {
        viewBinding.pdfView.savePenDrawing();
        showRemarkView(false, false);
    }

    private void cancelPenDrawing() {
        viewBinding.pdfView.cancelPenDrawing();
        showRemarkView(false, false);
    }

    private void clearPenDrawing() {
        viewBinding.pdfView.clearPenDrawing();
        showRemarkView(false, false);
    }


    private void requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, TheApplication.BASE_X5_PERMISSIONS)) {
            EasyPermissions.requestPermissions(
                    new PermissionRequest.Builder(this, TheApplication.REQUEST_X5_PERMISSIONS, TheApplication.BASE_X5_PERMISSIONS)
                            .setRationale(getString(R.string.permission_request_rationale))
                            .setPositiveButtonText(getString(R.string.permission_request_ok))
                            .setNegativeButtonText(getString(R.string.permission_request_cancel))
                            .build());
        }
    }

    private int[] getPopupWindowShowPosition(RectF startRect, RectF endRect, float translateX, float translateY, float targetViewSize) {
        int[] location = new int[2];
        viewBinding.pdfView.getLocationInWindow(location);

        float startTop = location[1] + startRect.top + translateY;
        float endBottom = location[1] + endRect.bottom + translateY;

        float startLeft = location[0] + startRect.left + translateX;
        float endRight = location[0] + endRect.right + translateX;

        int screenHeight = getScreenHeight(MainActivity.this);
        int x, y, isUp;
        if (endBottom <= screenHeight * 0.666) {
            x = (int) endRight;
            y = (int) (endBottom + targetViewSize);
            isUp = 1;
        } else {
            x = (int) startLeft;
            y = (int) startTop;
            isUp = 0;
        }
        return new int[]{x, y, isUp};
    }

    void openPdf(@NonNull String assetFileName, @Nullable String password) {
        configurator = viewBinding.pdfView.fromAsset(assetFileName);
        configurator.password(password)
                .defaultPage(pageNumber)
                .swipeHorizontal(true)
                .nightMode(false)
                .pageFling(false)
                .pageSnap(true)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .autoSpacing(true)
//                .scrollHandle(new DefaultScrollHandle(this))
//                .spacing(10) // in dp
                .onPageError(this)
                .pageFitPolicy(FitPolicy.WIDTH)
                .setAutoFillWhiteSpace(false)
                .setLoadAfterCheckWhiteSpace(true)
                .setUseMinWhiteSpaceZoom(false)
                .setWhiteSpaceRenderBestQuality(true)
                .setWhiteSpaceRenderThumbnailRatio(0.3f)
                .setWhiteSpaceRenderPageCountWhenOptimization(24)
                .setShowLoadingWhenWhiteSpaceRender(true)
                .setEditTextNormalColor(getResources().getColor(R.color.edit_text_remark_theme))
                .setEditTextRemarkThemeColor(getResources().getColor(R.color.edit_text_remark_theme))
                .setSingleZoom(true)
                .setReadOnlyMode(false)
                .load();
    }

    void showPopupWindowOperating(View view, int x, int y, boolean isUp, List<MarkAreaType> selectMarkAreaTypes) {
        popupWindowOperatingKey = PopupWindowUtil.POPUPWINDOWKEY + TimeUtil.getOnlyTimeWithoutSleep();
        PopupWindowUtil.showPopupWindowOperating(MainActivity.this, view.getRootView(), view, popupWindowOperatingKey, x, y, isUp, selectMarkAreaTypes,
                new PopupWindowUtil.OnShowPopupWindowOperatingListener() {
                    @Override
                    public boolean onSelect(PopupWindow popupWindow, View view, MarkAreaType markAreaType) {
                        return viewBinding.pdfView.drawSelectAreaWithMarkAreaType(markAreaType);
                    }

                    @Override
                    public boolean onCancelSelect(PopupWindow popupWindow, View view, MarkAreaType markAreaType) {
                        return viewBinding.pdfView.cancelSelectAreaAnnotation(markAreaType);
                    }

                    @Override
                    public void clearPage(PopupWindow popupWindow, View view) {
                        viewBinding.pdfView.clearPage();
                        popupWindow.dismiss();
                    }

                    @Override
                    public void onDismiss(PopupWindow popupWindow, View view) {
                        viewBinding.pdfView.dismissAreaSelect();
                    }
                });
    }

    private void doVibrate() {
        Vibrator vibrator = (Vibrator) this.getSystemService(Service.VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    void dismissPopupWindowBookMarket() {
        if (popupWindowBookMarketKey == null) {
            return;
        }
        PopupWindow popupWindow = getPopupWindow(popupWindowBookMarketKey);
        popupWindowBookMarketKey = null;
        if (popupWindow == null) {
            return;
        }
        popupWindow.dismiss();
    }

    void dismissPopupWindowOperating() {
        if (popupWindowOperatingKey == null) {
            return;
        }
        PopupWindow popupWindow = getPopupWindow(popupWindowOperatingKey);
        popupWindowOperatingKey = null;
        if (popupWindow == null) {
            return;
        }
        popupWindow.dismiss();
    }


    void visiblePopupWindowOperating(boolean isVisible) {
        if (popupWindowOperatingKey == null) {
            return;
        }
        PopupWindow popupWindow = getPopupWindow(popupWindowOperatingKey);
        if (popupWindow == null) {
            return;
        }
        popupWindow.getContentView().setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    void updatePopupWindowPosition(@NonNull RectF startRect, @NonNull RectF endRect, float translateX, float translateY, float targetViewSize, List<MarkAreaType> selectMarkAreaTypes) {
        if (popupWindowOperatingKey == null) {
            return;
        }
        PopupWindow popupWindow = getPopupWindow(popupWindowOperatingKey);
        if (popupWindow == null) {
            return;
        }
        View contentView = popupWindow.getContentView();
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        contentView.measure(spec, spec);
        int measuredWidth = contentView.getMeasuredWidth();
        int measuredHeight = contentView.getMeasuredHeight();
        final ImageView imageViewTriangleUp = (ImageView) contentView.findViewById(R.id.imageViewTriangleUp);
        final ImageView imageViewTriangleDown = (ImageView) contentView.findViewById(R.id.imageViewTriangleDown);
        final StatusView statusViewDeleteLine = (StatusView) contentView.findViewById(R.id.statusViewDeleteLine);
        final StatusView statusViewUnderLine = (StatusView) contentView.findViewById(R.id.statusViewUnderLine);
        final StatusView statusViewUnderWaveLine = (StatusView) contentView.findViewById(R.id.statusViewUnderWaveLine);
        final StatusView statusViewHighLight = (StatusView) contentView.findViewById(R.id.statusViewHighLight);
        statusViewDeleteLine.setChecked(false);
        statusViewUnderLine.setChecked(false);
        statusViewUnderWaveLine.setChecked(false);
        statusViewHighLight.setChecked(false);
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
        int[] position = getPopupWindowShowPosition(startRect, endRect, translateX, translateY, targetViewSize);
        if (position[2] == 1) {
            imageViewTriangleUp.setVisibility(View.VISIBLE);
            imageViewTriangleDown.setVisibility(View.GONE);
            popupWindow.update(position[0] - measuredWidth / 2, position[1], -1, -1);
        } else {
            imageViewTriangleUp.setVisibility(View.GONE);
            imageViewTriangleDown.setVisibility(View.VISIBLE);
            popupWindow.update(position[0] - measuredWidth / 2, position[1] - measuredHeight, -1, -1);
        }
    }


    private List<AnnotationBean> getAnnotationData() {
        List<AnnotationBean> annotations = new ArrayList<>();
        String saveJson = CacheDiskUtils.getInstance().getString(PDF_NAME, null);
        if (ObjectUtils.isNotEmpty(saveJson)) {
            annotations = GsonUtils.fromJson(saveJson, new TypeToken<List<AnnotationBean>>() {
            }.getType());
        }
        return annotations;
    }

    private void saveAnnotationData() {
         String result = GsonUtils.toJson(viewBinding.pdfView.getAllOptimizationAnnotation());
         CacheDiskUtils.getInstance().put(PDF_NAME, result);
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", getString(R.string.activity_main_page_tips), page + 1, pageCount));
        if (isActiveSearch && searchContent != null && !searchContent.equals("")) {
            searchText(searchContent);
        }
        viewBinding.layoutSearchAppbar.textViewToolbarCenter.setText(String.format("%s %s / %s", getString(R.string.activity_main_page_tips), page + 1, pageCount));
    }

    @Override
    public void loadComplete(int pageNumber) {

    }

    @Override
    public void onPageError(int page, Throwable t) {
        Log.e(getTag(), "Cannot load page " + page);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, new EasyPermissions.PermissionCallbacks() {
            @Override
            public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            }

            @Override
            public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
                switch (requestCode) {
                    case TheApplication.REQUEST_X5_PERMISSIONS:
                        if (perms.size() == TheApplication.BASE_X5_PERMISSIONS.length) {
                            TheApplication theApplication = TheApplication.getTheApplication();
                            if (theApplication.isNeedInitX5()) {
                                theApplication.initX5Web();
                            }
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
                if (EasyPermissions.somePermissionPermanentlyDenied(MainActivity.this, perms)) {
                    showAppSettingDialog();
                }
            }
        });
    }

    private void showAppSettingDialog() {
        new AppSettingsDialog.Builder(MainActivity.this).setRationale(getString(R.string.permission_request_open_setting_tips)).build().show();
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveAnnotationData();
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setMessage(getString(R.string.remark_change_tips));
        builder.setTitle(getString(R.string.tips));
        builder.show();
    }

    public void addRemoveAnnotation(String annotation, boolean isAdd) {
        if (annotation == null || annotation.equals("")) {
            return;
        }
        List<AnnotationBean> annotationBeans = new ArrayList<>();
        AnnotationBean annotationBean = getAnnotationData(annotation);
        if (annotationBean == null) {
            return;
        }
        annotationBeans.add(annotationBean);
        Log.e("addRemoveAnnotation", annotation);
        if (isAdd) {
            viewBinding.pdfView.addAnnotations(annotationBeans, true);
        } else {
            viewBinding.pdfView.removeAnnotations(annotationBeans, true);
        }
    }


    private AnnotationBean getAnnotationData(String annotation) {
        AnnotationBean annotationBean = null;
        if (annotation == null || annotation.equals("")) {
            return null;
        }
        try {
            annotationBean = GsonUtils.fromJson(annotation, new TypeToken<AnnotationBean>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return annotationBean;
    }


    @Override
    public void doBack() {
        super.doBack();
        showSaveDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        if (requestCode == THUMBNAIL_CHOOSE_REQUEST) {
            int page = intent.getIntExtra(RESULT_PAGE_KEY, -1);
            if (page != -1) {
                jumpToPageWithAutoFillCheck(page);
            }
        }
    }

    private void jumpToPageWithAutoFillCheck(int page){
        viewBinding.pdfView.addAnimationEndRunnable("jumpToPage",new Runnable() {
            @Override
            public void run() {
                viewBinding.pdfView.jumpToPageWithAutoFillCheck(page);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        String result = GsonUtils.toJson(viewBinding.pdfView.getAllOptimizationAnnotation());
        savedInstanceState.putString(SAVE_ANNOTATION_KEY, result);
    }

    @Override
    public void onDestroy() {
        if (viewBinding.pdfView != null) {
            viewBinding.pdfView.recycle();
        }
        super.onDestroy();
    }

}
