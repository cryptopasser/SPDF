/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.snakeway.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.snakeway.pdflibrary.PdfDocument;
import com.snakeway.pdflibrary.PdfiumCore;
import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.annotation.AnnotationBean;
import com.snakeway.pdfviewer.annotation.AnnotationListener;
import com.snakeway.pdfviewer.annotation.MarkAnnotation;
import com.snakeway.pdfviewer.annotation.PenAnnotation;
import com.snakeway.pdfviewer.annotation.TextAnnotation;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.base.MarkAreaType;
import com.snakeway.pdfviewer.annotation.eraser.Eraser;
import com.snakeway.pdfviewer.annotation.pen.AreaPen;
import com.snakeway.pdfviewer.annotation.pen.Pen;
import com.snakeway.pdfviewer.annotation.pen.SearchAreaPen;
import com.snakeway.pdfviewer.annotation.pen.TextPen;
import com.snakeway.pdfviewer.exception.PageRenderingException;
import com.snakeway.pdfviewer.link.DefaultLinkHandler;
import com.snakeway.pdfviewer.link.LinkHandler;
import com.snakeway.pdfviewer.listener.Callbacks;
import com.snakeway.pdfviewer.listener.OnAreaTouchListener;
import com.snakeway.pdfviewer.listener.OnDrawListener;
import com.snakeway.pdfviewer.listener.OnErrorListener;
import com.snakeway.pdfviewer.listener.OnLoadCompleteListener;
import com.snakeway.pdfviewer.listener.OnLongPressListener;
import com.snakeway.pdfviewer.listener.OnPageChangeListener;
import com.snakeway.pdfviewer.listener.OnPageErrorListener;
import com.snakeway.pdfviewer.listener.OnPageScrollListener;
import com.snakeway.pdfviewer.listener.OnRenderListener;
import com.snakeway.pdfviewer.listener.OnSearchTextListener;
import com.snakeway.pdfviewer.listener.OnTapListener;
import com.snakeway.pdfviewer.listener.OnTextRemarkListener;
import com.snakeway.pdfviewer.model.PagePart;
import com.snakeway.pdfviewer.model.RenderedBitmap;
import com.snakeway.pdfviewer.model.SearchTextInfo;
import com.snakeway.pdfviewer.model.TextRemarkInfo;
import com.snakeway.pdfviewer.model.WhiteSpaceInfo;
import com.snakeway.pdfviewer.scroll.ScrollHandle;
import com.snakeway.pdfviewer.source.AssetSource;
import com.snakeway.pdfviewer.source.ByteArraySource;
import com.snakeway.pdfviewer.source.DocumentSource;
import com.snakeway.pdfviewer.source.FileSource;
import com.snakeway.pdfviewer.source.InputStreamSource;
import com.snakeway.pdfviewer.source.UriSource;
import com.snakeway.pdfviewer.util.Base64Util;
import com.snakeway.pdfviewer.util.BitmapMemoryCacheHelper;
import com.snakeway.pdfviewer.util.BitmapUtil;
import com.snakeway.pdfviewer.util.Constants;
import com.snakeway.pdfviewer.util.EditTextUtil;
import com.snakeway.pdfviewer.util.FitPolicy;
import com.snakeway.pdfviewer.util.MathUtils;
import com.snakeway.pdfviewer.util.SnapEdge;
import com.snakeway.pdfviewer.util.UnicodeUtil;
import com.snakeway.pdfviewer.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.snakeway.pdfviewer.util.Constants.THUMBNAIL_RATIO;

/**
 * It supports animations, zoom, cache, and swipe.
 * <p>
 * To fully understand this class you must know its principles :
 * - The PDF document is seen as if we always want to draw all the pages.
 * - The thing is that we only draw the visible parts.
 * - All parts are the same size, this is because we can't interrupt a native page rendering,
 * so we need these renderings to be as fast as possible, and be able to interrupt them
 * as soon as we can.
 * - The parts are loaded when the current offset or the current zoom level changes
 * <p>
 * Important :
 * - DocumentPage = A page of the PDF document.
 * - UserPage = A page as defined by the user.
 * By default, they're the same. But the user can change the pages order
 * using {@link #load(DocumentSource, String, int[])}. In this
 * particular case, a userPage of 5 can refer to a documentPage of 17.
 */
public class PDFView extends RelativeLayout {

    private static final String TAG = PDFView.class.getSimpleName();

    public static final float DEFAULT_MAX_SCALE = 3.0f;
    public static final float DEFAULT_MID_SCALE = 1.75f;
    public static final float DEFAULT_MIN_SCALE = 1.0f;

    public static final int TOUCH_SLOP = 12;

    private float minZoom = DEFAULT_MIN_SCALE;
    private float midZoom = DEFAULT_MID_SCALE;
    private float maxZoom = DEFAULT_MAX_SCALE;

    private float defaultZoom = 1f;

    private final String START_ZOOM_ANIMATION = "startZoomAnimation";

    /**
     * START - scrolling in first page direction
     * END - scrolling in last page direction
     * NONE - not scrolling
     */
    enum ScrollDir {
        NONE, START, END
    }

    public enum PdfViewClickType {
        PEN_DRAW, PEN_CANCEL, PEN_CANCEL_All, EXTRA
    }

    private ScrollDir scrollDir = ScrollDir.NONE;

    /**
     * Rendered parts go to the cache manager
     */
    CacheManager cacheManager;

    /**
     * Animation manager manage all offset and zoom animation
     */
    AnimationManager animationManager;

    /**
     * Drag manager manage all touch events
     */
    DragPinchManager dragPinchManager;

    AnnotationManager annotationManager;

    AnnotationDrawManager annotationDrawManager;

    public PdfFile pdfFile;

    /**
     * The index of the current sequence
     */
    private int currentPage;

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    private float currentXOffset = 0;

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    private float currentYOffset = 0;

    /**
     * The zoom level, always >= 1
     */
    private float zoom = defaultZoom;

    /**
     * True if the PDFView has been recycled
     */
    private boolean recycled = true;

    /**
     * Current state of the view
     */
    private State state = State.DEFAULT;

    private Function function = Function.VIEWER;

    /**
     * Async task used during the loading phase to decode a PDF document
     */
    private DecodingAsyncTask decodingAsyncTask;

    private SearchTextAsyncTask searchTextAsyncTask;

    /**
     * The thread {@link #renderingHandler} will run on
     */
    private HandlerThread renderingHandlerThread;
    /**
     * Handler always waiting in the background and rendering tasks
     */
    RenderingHandler renderingHandler;

    private HandlerThread whiteSpaceInfoHandlerThread;

    WhiteSpaceInfoHandler whiteSpaceInfoHandler;

    private HandlerThread renderingCustomHandlerThread;

    RenderingCustomHandler renderingCustomHandler;

    private PagesLoader pagesLoader;

    Callbacks callbacks = new Callbacks();

    /**
     * Paint object for drawing
     */
    private Paint paint;

    /**
     * Paint object for drawing debug stuff
     */
    private Paint debugPaint;

    /**
     * Policy for fitting pages to screen
     */
    private FitPolicy pageFitPolicy = FitPolicy.WIDTH;

    private boolean fitEachPage = false;

    private int defaultPage = 0;

    /**
     * True if should scroll through pages vertically instead of horizontally
     */
    private boolean swipeVertical = true;

    private boolean enableSwipe = true;

    private boolean doubletapEnabled = true;

    private boolean nightMode = false;

    private boolean pageSnap = true;

    private boolean enablePenAnnotationClickCheckOnViewMode = true;

    private boolean enableTextAnnotationClickCheckOnViewMode = true;

    /**
     * Pdfium core for loading and rendering PDFs
     */
    PdfiumCore pdfiumCore;

    private ScrollHandle scrollHandle;

    private boolean isScrollHandleInit = false;

    ScrollHandle getScrollHandle() {
        return scrollHandle;
    }

    /**
     * True if bitmap should use ARGB_8888 format and take more memory
     * False if bitmap should be compressed by using RGB_565 format and take less memory
     */
    private boolean bestQuality = false;

    /**
     * True if annotations should be rendered
     * False otherwise
     */
    private boolean annotationRendering = false;

    /**
     * True if the view should render during scaling<br/>
     * Can not be forced on older API versions (< Build.VERSION_CODES.KITKAT) as the GestureDetector does
     * not detect scrolling while scaling.<br/>
     * False otherwise
     */
    private boolean renderDuringScale = false;

    /**
     * Antialiasing and bitmap filtering
     */
    private boolean enableAntialiasing = true;
    private PaintFlagsDrawFilter antialiasFilter =
            new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    /**
     * Spacing between pages, in px
     */
    private int spacingPx = 0;

    /**
     * Add dynamic spacing to fit each page separately on the screen.
     */
    private boolean autoSpacing = false;

    private boolean drawingPenOptimize = false;

    /**
     * Fling a single page at a time
     */
    private boolean pageFling = true;

    /**
     * Pages numbers used when calling onDrawAllListener
     */
    public final List<Integer> onDrawPagesNums = new ArrayList<>(10);


    /**
     * Holds info whether view has been added to layout and has width and height
     */
    private boolean hasSize = false;

    /**
     * Holds last used Configurator that should be loaded when view has size
     */
    private Configurator waitingDocumentConfigurator;

    private float lastMotionX;

    private float lastMotionY;

    private int counter;

    private boolean isReleased;

    private boolean isMoved;

    //预览pdf时候长按进行内容选择,激活内容选择
    private boolean activeAreaAnnotation;

    private float penDrawCheckLastMotionX;

    private float penDrawCheckLastMotionY;

    private int penDrawCheckCounter;

    private boolean penDrawCheckIsReleased;

    private boolean penDrawCheckIsMoved;

    private boolean penDrawLongClickActive;

    private OnAreaTouchListener onAreaTouchListener;

    private float textPenDrawCheckLastMotionX;

    private float textPenDrawCheckLastMotionY;

    private int textPenDrawCheckCounter;

    private boolean textPenDrawCheckIsReleased;

    private boolean textPenDrawCheckIsMoved;

    private boolean textPenDrawLongClickActive;

    private OnTextRemarkListener onTextRemarkListener;

    private OnClickListener onPdfViewClickListener;

    boolean startTargetSelect = false;

    boolean endTargetSelect = false;

    boolean reTouchStart = false;

    boolean isMoveEnd = true;

    private boolean readOnlyMode = false;

    private int annotationRenderingArea = 1;

    private boolean autoFillWhiteSpace = false;

    private boolean supportCustomRendering = false;

    private boolean loadAfterCheckWhiteSpace = false;

    private boolean touchWithoutSpace = false;

    private final List<WhiteSpaceInfo> whiteSpaceInfos = new ArrayList<>();

    public final Handler handler = new Handler(Looper.getMainLooper());

    public final Handler autoFillWhiteSpaceHandler = new Handler(Looper.getMainLooper());

    private boolean isAutoFillWhiteSpaceZooming = false;

    private boolean useMinWhiteSpaceZoom = false;

    private boolean initWhiteSpaceOptimization = true;

    private boolean isWhiteSpaceRenderBestQuality = false;

    private float whiteSpaceRenderThumbnailRatio = THUMBNAIL_RATIO;

    private int whiteSpaceRenderPageCountWhenOptimization = 12;

    private Bitmap cancelBitmap = null;

    private float cancelBitmapSize = 50;

    private float editTextRemarkFontSize = 12;

    private int editTextRemarkThemeColor;

    private int editTextNormalColor;

    private int editTextHintColor;

    private boolean showLoadingWhenWhiteSpaceRender = false;

    private boolean initWhiteSpaceRender = false;

    public boolean doZooming = false;

    public boolean doMoving = false;

    public FrameLayout frameLayoutProgressBar;

    public RelativeLayout relativeLayoutTextRemarkView;

    public FrameLayout frameLayoutTextRemarkBackground;

    public LinearLayout linearLayoutTextRemarkContentView;

    public LinearLayout linearLayoutOperatingTop;

    public LinearLayout linearLayoutOperatingBottom;

    public EditText editTextTextRemark;

    public TextView textViewTextRemarkCancelTop;
    public TextView textViewTextRemarkCancelBottom;

    public TextView textViewTextRemarkDeleteTop;
    public TextView textViewTextRemarkDeleteBottom;

    public TextView textViewTextRemarkSaveTop;
    public TextView textViewTextRemarkSaveBottom;

    public List<PenAnnotation> penAnnotations = new ArrayList<>();

    public CustomRenderingView customRenderingView;

    public CustomPenDrawingView customPenHaveDrawingView;

    public CustomPenDrawingView customPenDrawingView;
    public BitmapMemoryCacheHelper bitmapMemoryCacheHelper = new BitmapMemoryCacheHelper(8);

    private TextAnnotation editTextAnnotation;

    private float pdfFontUnit = 1;

    private HashMap<String, Runnable> animationEndRunnables = new HashMap<String, Runnable>();

    private boolean singleZoom = false;

    private boolean easyLoader = false;

    /**
     * Construct the initial view
     */
    public PDFView(Context context, AttributeSet set) {
        super(context, set);

        renderingHandlerThread = new HandlerThread("PDF renderer");
        whiteSpaceInfoHandlerThread = new HandlerThread("White space calculation");
        renderingCustomHandlerThread = new HandlerThread("Custom rendering");
        if (isInEditMode()) {
            return;
        }

        initCustomRenderingView();
        initCustomPenDrawingView();

        initTextRemarkView();
        initProgressBar();
        cacheManager = new CacheManager();
        animationManager = new AnimationManager(this);
        dragPinchManager = new DragPinchManager(this, animationManager);
        annotationManager = new AnnotationManager(this);
        annotationDrawManager = new AnnotationDrawManager(this, annotationManager);
        pagesLoader = new PagesLoader(this);

        paint = new Paint();
        debugPaint = new Paint();
        debugPaint.setStyle(Style.STROKE);

        pdfiumCore = new PdfiumCore(context);
        setWillNotDraw(false);
    }

    private void load(DocumentSource docSource, String password) {
        load(docSource, password, null);
    }

    private void load(DocumentSource docSource, String password, int[] userPages) {
        PDFView.this.updateEditTextRemarkView();
        if (!recycled) {
            throw new IllegalStateException("Don't call load on a PDF View without recycling it first.");
        }

        recycled = false;
        // Start decoding document
        decodingAsyncTask = new DecodingAsyncTask(docSource, password, userPages, this, pdfiumCore);
        decodingAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void initProgressBar() {
        View progressView = LayoutInflater.from(getContext()).inflate(R.layout.view_loading_progress, this, true);
        frameLayoutProgressBar = (FrameLayout) progressView.findViewById(R.id.frameLayoutProgressBar);
        frameLayoutProgressBar.setVisibility(GONE);
    }

    public void setProgressViewBackground(int color) {
        frameLayoutProgressBar.setBackgroundColor(color);
    }

    private void initCustomRenderingView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_custom_rendering_root, this, true);
        customRenderingView = (CustomRenderingView) view.findViewById(R.id.customRenderingView);
        customRenderingView.initPdfView(this);
    }

    private void initCustomPenDrawingView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_custom_pen_drawing, this, true);
        customPenHaveDrawingView = (CustomPenDrawingView) view.findViewById(R.id.customPenHaveDrawingView);
        customPenDrawingView = (CustomPenDrawingView) view.findViewById(R.id.customPenDrawingView);
        customPenHaveDrawingView.initPdfView(this);
        customPenDrawingView.initPdfView(this);
    }

    private void initTextRemarkView() {
        View textRemarkView = LayoutInflater.from(getContext()).inflate(R.layout.view_edittext_remark, this, true);
        relativeLayoutTextRemarkView = (RelativeLayout) textRemarkView.findViewById(R.id.relativeLayoutTextRemarkView);
        frameLayoutTextRemarkBackground = (FrameLayout) textRemarkView.findViewById(R.id.frameLayoutTextRemarkBackground);
        linearLayoutTextRemarkContentView = (LinearLayout) textRemarkView.findViewById(R.id.linearLayoutTextRemarkContentView);
        linearLayoutOperatingTop = (LinearLayout) textRemarkView.findViewById(R.id.linearLayoutOperatingTop);
        linearLayoutOperatingBottom = (LinearLayout) textRemarkView.findViewById(R.id.linearLayoutOperatingBottom);
        editTextTextRemark = (EditText) textRemarkView.findViewById(R.id.editTextTextRemark);

        textViewTextRemarkCancelTop = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkCancelTop);
        textViewTextRemarkDeleteTop = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkDeleteTop);
        textViewTextRemarkSaveTop = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkSaveTop);

        textViewTextRemarkCancelBottom = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkCancelBottom);
        textViewTextRemarkDeleteBottom = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkDeleteBottom);
        textViewTextRemarkSaveBottom = (TextView) textRemarkView.findViewById(R.id.textViewTextRemarkSaveBottom);

        relativeLayoutTextRemarkView.setVisibility(GONE);
        frameLayoutTextRemarkBackground.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextAnnotation == null) {
                    return;
                }
                resetEditTextAnnotation();
                editTextTextRemark.setText("");
                relativeLayoutTextRemarkView.setVisibility(GONE);
                if (onTextRemarkListener != null) {
                    onTextRemarkListener.onCancel(editTextTextRemark, true);
                }
            }
        });
        OnClickListener onClickListenerCancel = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextAnnotation != null) {
                    resetEditTextAnnotation();
                }
                editTextTextRemark.setText("");
                relativeLayoutTextRemarkView.setVisibility(GONE);
                if (onTextRemarkListener != null) {
                    onTextRemarkListener.onCancel(editTextTextRemark, false);
                }
            }
        };
        textViewTextRemarkCancelTop.setOnClickListener(onClickListenerCancel);
        textViewTextRemarkCancelBottom.setOnClickListener(onClickListenerCancel);

        OnClickListener onClickListenerDelete = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextAnnotation == null) {
                    return;
                }
                List<BaseAnnotation> annotations = new ArrayList<>();
                annotations.add(editTextAnnotation);
                annotationManager.removeAnnotations(annotations, true);

                resetEditTextAnnotation();
                editTextTextRemark.setText("");
                relativeLayoutTextRemarkView.setVisibility(GONE);
                if (onTextRemarkListener != null) {
                    onTextRemarkListener.onDelete(editTextTextRemark, editTextAnnotation);
                }
            }
        };
        textViewTextRemarkDeleteTop.setOnClickListener(onClickListenerDelete);
        textViewTextRemarkDeleteBottom.setOnClickListener(onClickListenerDelete);

        OnClickListener onClickListenerSave = new OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = editTextTextRemark.getText().toString();
                float[] position = new float[3];
                if (linearLayoutTextRemarkContentView.getTag() != null) {
                    position = (float[]) linearLayoutTextRemarkContentView.getTag();
                }
//                RelativeLayout.MarginLayoutParams layoutParams = (RelativeLayout.MarginLayoutParams) linearLayoutTextRemarkContentView.getLayoutParams();
                int x = (int) (position[0]);
                int y = (int) (position[1]);
                TextRemarkInfo textRemarkInfo = new TextRemarkInfo(UUID.randomUUID().toString(), data, x, y, zoom, getCurrentPage());

                if (editTextAnnotation != null) {
                    List<BaseAnnotation> annotations = new ArrayList<>();
                    annotations.add(editTextAnnotation);
                    annotationManager.removeAnnotations(annotations, true);
                    annotationManager.saveTextPenAnnotation(textRemarkInfo, true);
                    resetEditTextAnnotation();
                } else {
                    annotationManager.saveTextPenAnnotation(textRemarkInfo, true);
                }
                editTextTextRemark.setText("");
                relativeLayoutTextRemarkView.setVisibility(GONE);
                if (onTextRemarkListener != null) {
                    onTextRemarkListener.onSave(editTextTextRemark, textRemarkInfo);
                }
            }
        };
        textViewTextRemarkSaveTop.setOnClickListener(onClickListenerSave);
        textViewTextRemarkSaveBottom.setOnClickListener(onClickListenerSave);

        editTextTextRemark.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void resetEditTextAnnotation() {
        if (editTextAnnotation != null) {
            editTextAnnotation.setNeedHidden(false);
            editTextAnnotation = null;
            redraw();
        }
    }

    public void updateEditTextRemarkView() {
        if (editTextNormalColor != 0) {
            editTextTextRemark.setTextColor(editTextNormalColor);
        }
        if (editTextHintColor != 0) {
            editTextTextRemark.setHintTextColor(editTextHintColor);
        }
        if (editTextRemarkThemeColor != 0) {
            textViewTextRemarkCancelTop.setTextColor(editTextRemarkThemeColor);
            textViewTextRemarkDeleteTop.setTextColor(editTextRemarkThemeColor);
            textViewTextRemarkSaveTop.setTextColor(editTextRemarkThemeColor);
            textViewTextRemarkCancelBottom.setTextColor(editTextRemarkThemeColor);
            textViewTextRemarkDeleteBottom.setTextColor(editTextRemarkThemeColor);
            textViewTextRemarkSaveBottom.setTextColor(editTextRemarkThemeColor);
        }
    }

    public Bitmap getTextRemarkBitmapCache(String key, String data, int color, float zoom, boolean needCache) {
        if (needCache) {
            Bitmap bitmap = bitmapMemoryCacheHelper.getBitmap(key);
            if (bitmap != null) {
                return bitmap;
            }
        }
        View viewTextRemark = LayoutInflater.from(getContext()).inflate(R.layout.view_text_remark, customRenderingView, false);
        TextView textView = (TextView) viewTextRemark.findViewById(R.id.textViewRemark);
        textView.setText(data);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        int margin = (int) (editTextRemarkFontSize * pdfFontUnit * zoom / 2);
        viewTextRemark.setPadding(margin, margin, margin, margin);

        if (color != 0) {
            textView.setTextColor(color);
        }
        viewTextRemark.setTag(key);
        Bitmap viewBitmap = BitmapUtil.getViewBitmap(viewTextRemark);
        if (viewBitmap == null) {
            return null;
        }
        if (needCache) {
            bitmapMemoryCacheHelper.putBitmap(key, viewBitmap);
        }
        return viewBitmap;
    }

    private List<View> getAllChildViews(ViewGroup viewGroup) {
        List<View> childViews = new ArrayList();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View viewChild = viewGroup.getChildAt(i);
            childViews.add(viewChild);
        }
        return childViews;
    }


    private void moveTextRemarkView(float x, float y) {
        if (relativeLayoutTextRemarkView.getVisibility() != VISIBLE) {
            relativeLayoutTextRemarkView.setVisibility(VISIBLE);
            if (onTextRemarkListener != null) {
                onTextRemarkListener.onShow(editTextTextRemark);
            }
        }
        MarginLayoutParams layoutParams = (MarginLayoutParams) linearLayoutTextRemarkContentView.getLayoutParams();
        float[] position = new float[3];
        position[0] = x;
        position[1] = y;
        if ((getWidth() > getHeight() && y > (float) (getHeight() / 3)) || (getWidth() < getHeight() && y > (float) (getHeight() * 2 / 5))) {
            linearLayoutOperatingTop.setVisibility(VISIBLE);
            linearLayoutOperatingBottom.setVisibility(GONE);

            int widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            linearLayoutOperatingTop.measure(widthSpec, heightSpec);
            int height = linearLayoutOperatingTop.getMeasuredHeight();

            position[2] = height;
            y = y - position[2];
        } else {
            linearLayoutOperatingTop.setVisibility(GONE);
            linearLayoutOperatingBottom.setVisibility(VISIBLE);
            position[2] = 0;
        }
        if (editTextAnnotation != null) {
            textViewTextRemarkCancelTop.setVisibility(GONE);
            textViewTextRemarkCancelBottom.setVisibility(GONE);
            textViewTextRemarkDeleteTop.setVisibility(VISIBLE);
            textViewTextRemarkDeleteBottom.setVisibility(VISIBLE);
            frameLayoutTextRemarkBackground.setVisibility(VISIBLE);
            textViewTextRemarkSaveTop.setText(getResources().getString(R.string.view_text_remark_operating_edit));
            textViewTextRemarkSaveBottom.setText(getResources().getString(R.string.view_text_remark_operating_edit));
            TextRemarkInfo textRemarkInfo = editTextAnnotation.data;
            if (textRemarkInfo != null) {
                editTextTextRemark.setText(textRemarkInfo.getData());
                EditTextUtil.setCursorToLast(editTextTextRemark);
            }
        } else {
            textViewTextRemarkCancelTop.setVisibility(VISIBLE);
            textViewTextRemarkCancelBottom.setVisibility(VISIBLE);
            textViewTextRemarkDeleteTop.setVisibility(GONE);
            textViewTextRemarkDeleteBottom.setVisibility(GONE);
            frameLayoutTextRemarkBackground.setVisibility(GONE);
            textViewTextRemarkSaveTop.setText(getResources().getString(R.string.view_text_remark_operating_save));
            textViewTextRemarkSaveBottom.setText(getResources().getString(R.string.view_text_remark_operating_save));
        }
        //  int[] res=  annotationManager.getTargetPdfXY((int)x,(int)y);
        linearLayoutTextRemarkContentView.setTag(position);
        layoutParams.setMargins((int) x, (int) y, 0, 0);
        linearLayoutTextRemarkContentView.requestLayout();
    }

    /**
     * Go to the given page.
     *
     * @param page Page index.
     */
    public void jumpTo(int page, boolean withAnimation) {
        if (recycled || pdfFile == null) {
            return;
        }
        page = pdfFile.determineValidPageNumberFrom(page);
        SnapEdge edge = isAutoFillWhiteSpace() ? SnapEdge.CENTER : findSnapEdge(page);
        float offset = snapOffsetForPage(page, edge);
        if (withAnimation) {
            if (swipeVertical) {
                animationManager.startYAnimation(currentYOffset, -offset);
            } else {
                animationManager.startXAnimation(currentXOffset, -offset);
            }
        } else {
            if (swipeVertical) {
                moveTo(currentXOffset, -offset);
            } else {
                moveTo(-offset, currentYOffset);
            }
        }
        showPage(page, withAnimation);
    }

    public void jumpTo(int page) {
        jumpTo(page, false);
    }

    void showPage(int pageNb, boolean withAnimation) {
        if (recycled || pdfFile == null) {
            return;
        }
        // Check the page number and makes the
        // difference between UserPages and DocumentPages
        pageNb = pdfFile.determineValidPageNumberFrom(pageNb);

        currentPage = pageNb;
        loadPages();

        if (scrollHandle != null && !documentFitsView()) {
            scrollHandle.setPageNum(currentPage + 1);
        }

        callbacks.callOnPageChange(currentPage, pdfFile.getPagesCount());
        if (!autoFillWhiteSpace) {
            return;
        }
        float theZoom = getWhiteSpaceZoom(currentPage);
        if (zoom != theZoom) {
            if (withAnimation) {
                isAutoFillWhiteSpaceZooming = true;
                autoFillWhiteSpaceHandler.removeCallbacksAndMessages(null);
                autoFillWhiteSpaceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        zoomWithAnimation(theZoom);
                        isAutoFillWhiteSpaceZooming = false;
                    }
                }, 500);
            } else {
                zoomCenteredTo(theZoom, new PointF(getWidth() / 2, getHeight() / 2));
            }
        }
    }

    /**
     * Get current position as ratio of document length to visible area.
     * 0 means that document start is visible, 1 that document end is visible
     *
     * @return offset between 0 and 1
     */
    public float getPositionOffset() {
        if (recycled || pdfFile == null) {
            return 0;
        }
        float offset;
        if (swipeVertical) {
            offset = -currentYOffset / (pdfFile.getDocLen(zoom) - getHeight());
        } else {
            offset = -currentXOffset / (pdfFile.getDocLen(zoom) - getWidth());
        }
        return MathUtils.limit(offset, 0, 1);
    }

    /**
     * @param progress   must be between 0 and 1
     * @param moveHandle whether to move scroll handle
     * @see PDFView#getPositionOffset()
     */
    public void setPositionOffset(float progress, boolean moveHandle) {
        if (recycled || pdfFile == null) {
            return;
        }
        if (swipeVertical) {
            moveTo(currentXOffset, (-pdfFile.getDocLen(zoom) + getHeight()) * progress, moveHandle);
        } else {
            moveTo((-pdfFile.getDocLen(zoom) + getWidth()) * progress, currentYOffset, moveHandle);
        }
        loadPageByOffset();
    }

    public void setPositionOffset(float progress) {
        setPositionOffset(progress, true);
    }

    public void stopFling() {
        animationManager.stopFling();
    }

    public int getPageCount() {
        if (recycled || pdfFile == null) {
            return 0;
        }
        return pdfFile.getPagesCount();
    }

    public void setSwipeEnabled(boolean enableSwipe) {
        this.enableSwipe = enableSwipe;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
        if (nightMode) {
            ColorMatrix colorMatrixInverted =
                    new ColorMatrix(new float[]{
                            -1, 0, 0, 0, 255,
                            0, -1, 0, 0, 255,
                            0, 0, -1, 0, 255,
                            0, 0, 0, 1, 0});

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrixInverted);
            paint.setColorFilter(filter);
        } else {
            paint.setColorFilter(null);
        }
    }

    void enableDoubletap(boolean enableDoubletap) {
        this.doubletapEnabled = enableDoubletap;
    }

    boolean isDoubletapEnabled() {
        return doubletapEnabled;
    }

    void onPageError(PageRenderingException ex) {
        if (!callbacks.callOnPageError(ex.getPage(), ex.getCause())) {
            Log.e(TAG, "Cannot open page " + ex.getPage(), ex.getCause());
        }
    }

    public void recycle() {
        waitingDocumentConfigurator = null;

        animationManager.stopAll();
        dragPinchManager.disable();

        // Stop tasks
        if (renderingHandler != null) {
            renderingHandler.stop();
            renderingHandler.removeMessages(RenderingHandler.MSG_RENDER_TASK);
            while (renderingHandler.getRequestCount() != 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            renderingHandler = null;
        }
        if (renderingCustomHandler != null) {
            renderingCustomHandler.stop();
            renderingCustomHandler.removeMessages(RenderingCustomHandler.MSG_RENDERING_CUSTOM_TASK);
            while (renderingCustomHandler.getRequestCount() != 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            renderingCustomHandler = null;
        }
        if (whiteSpaceInfoHandler != null) {
            whiteSpaceInfoHandler.stop();
            whiteSpaceInfoHandler.removeMessages(WhiteSpaceInfoHandler.MSG_WHITE_SPACE_TASK);
            while (whiteSpaceInfoHandler.getRequestCount() != 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (frameLayoutProgressBar.getVisibility() == View.VISIBLE) {
                frameLayoutProgressBar.setVisibility(GONE);
            }
            whiteSpaceInfoHandler = null;
        }
        if (decodingAsyncTask != null) {
            decodingAsyncTask.cancel(true);
        }

        // Clear caches
        cacheManager.recycle();

        if (scrollHandle != null && isScrollHandleInit) {
            scrollHandle.destroyLayout();
        }

        if (pdfFile != null) {
            pdfFile.dispose();
            pdfFile = null;
        }

        scrollHandle = null;
        isScrollHandleInit = false;
        currentXOffset = currentYOffset = 0;
        zoom = defaultZoom;
        recycled = true;
        callbacks = new Callbacks();
        state = State.DEFAULT;
    }

    public boolean isRecycled() {
        return recycled;
    }

    /**
     * Handle fling animation
     */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (isInEditMode()) {
            return;
        }
        animationManager.computeFling();
    }

    @Override
    protected void onDetachedFromWindow() {
        recycle();
        handler.removeCallbacksAndMessages(null);
        autoFillWhiteSpaceHandler.removeCallbacksAndMessages(null);
        if (renderingHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                renderingHandlerThread.quitSafely();
            } else {
                renderingHandlerThread.quit();
            }
            renderingHandlerThread = null;
        }
        if (whiteSpaceInfoHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                whiteSpaceInfoHandlerThread.quitSafely();
            } else {
                whiteSpaceInfoHandlerThread.quit();
            }
            whiteSpaceInfoHandlerThread = null;
        }
        if (renderingCustomHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                renderingCustomHandlerThread.quitSafely();
            } else {
                renderingCustomHandlerThread.quit();
            }
            renderingCustomHandlerThread = null;
        }
        if (searchTextAsyncTask != null) {
            searchTextAsyncTask.cancel(true);
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        hasSize = true;
        if (waitingDocumentConfigurator != null) {
            waitingDocumentConfigurator.load();
        }
        if (isInEditMode() || state != State.SHOWN || recycled || pdfFile == null) {
            return;
        }

        // calculates the position of the point which in the center of view relative to big strip
        float centerPointInStripXOffset = -currentXOffset + oldw * 0.5f;
        float centerPointInStripYOffset = -currentYOffset + oldh * 0.5f;

        float relativeCenterPointInStripXOffset;
        float relativeCenterPointInStripYOffset;

        if (swipeVertical) {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile.getMaxPageWidth(getCurrentPage());
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile.getDocLen(zoom);
        } else {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile.getDocLen(zoom);
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile.getMaxPageHeight(getCurrentPage());
        }

        animationManager.stopAll();
        pdfFile.recalculatePageSizes(new Size(w, h));

        if (swipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.getMaxPageWidth(getCurrentPage()) + w * 0.5f;
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.getDocLen(zoom) + h * 0.5f;
        } else {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile.getDocLen(zoom) + w * 0.5f;
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile.getMaxPageHeight(getCurrentPage()) + h * 0.5f;
        }
        moveTo(currentXOffset, currentYOffset);
        loadPageByOffset();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (recycled || pdfFile == null) {
            return true;
        }
        if (swipeVertical) {
            if (direction < 0 && currentXOffset < 0) {
                return true;
            } else if (direction > 0 && currentXOffset + toCurrentScale(pdfFile.getMaxPageWidth(getCurrentPage())) > getWidth()) {
                return true;
            }
        } else {
            if (direction < 0 && currentXOffset < 0) {
                return true;
            } else if (direction > 0 && currentXOffset + pdfFile.getDocLen(zoom) > getWidth()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (recycled) {
            return true;
        }
        if (swipeVertical) {
            if (direction < 0 && currentYOffset < 0) {
                return true;
            } else if (direction > 0 && currentYOffset + pdfFile.getDocLen(zoom) > getHeight()) {
                return true;
            }
        } else {
            if (direction < 0 && currentYOffset < 0) {
                return true;
            } else if (direction > 0 && currentYOffset + toCurrentScale(pdfFile.getMaxPageHeight(getCurrentPage())) > getHeight()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        // As I said in this class javadoc, we can think of this canvas as a huge
        // strip on which we draw all the images. We actually only draw the rendered
        // parts, of course, but we render them in the place they belong in this huge
        // strip.

        // That's where Canvas.translate(x, y) becomes very helpful.
        // This is the situation :
        //  _______________________________________________
        // |   			 |					 			   |
        // | the actual  |					The big strip  |
        // |	canvas	 | 								   |
        // |_____________|								   |
        // |_______________________________________________|
        //
        // If the rendered part is on the bottom right corner of the strip
        // we can draw it but we won't see it because the canvas is not big enough.

        // But if we call translate(-X, -Y) on the canvas just before drawing the object :
        //  _______________________________________________
        // |   			  					  _____________|
        // |   The big strip     			 |			   |
        // |		    					 |	the actual |
        // |								 |	canvas	   |
        // |_________________________________|_____________|
        //
        // The object will be on the canvas.
        // This technique is massively used in this method, and allows
        // abstraction of the screen position when rendering the parts.

        // Draws background

        if (enableAntialiasing) {
            canvas.setDrawFilter(antialiasFilter);
        }

        Drawable bg = getBackground();
        if (bg == null) {
            canvas.drawColor(nightMode ? Color.BLACK : Color.WHITE);
        } else {
            bg.draw(canvas);
        }

        if (recycled) {
            return;
        }

        if (state != State.SHOWN) {
            return;
        }

        // Moves the canvas before drawing any element
        float currentXOffset = this.currentXOffset;
        float currentYOffset = this.currentYOffset;
        canvas.translate(currentXOffset, currentYOffset);

        // Draws thumbnails
        for (PagePart part : cacheManager.getThumbnails()) {
            drawPart(canvas, part);
        }
        // Draws parts
        for (PagePart part : cacheManager.getPageParts()) {
            drawPart(canvas, part);
            if (callbacks.getOnDrawAll() != null
                    && !onDrawPagesNums.contains(part.getPage())) {
                onDrawPagesNums.add(part.getPage());
            }
//            if (!onDrawAnnotationPagesNums.contains(part.getPage())) {
//                onDrawAnnotationPagesNums.add(part.getPage());
//            }
        }

        zoomTextRemarkTextSize();
//        annotationDrawManager.recycle(onDrawAnnotationPagesNums);
//        for (Integer page : onDrawAnnotationPagesNums) {
//            annotationDrawManager.draw(canvas, page);
//        }
//        onDrawAnnotationPagesNums.clear();

        for (Integer page : onDrawPagesNums) {
            drawWithListener(canvas, page, callbacks.getOnDrawAll());
        }
        onDrawPagesNums.clear();

        drawWithListener(canvas, currentPage, callbacks.getOnDraw());

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset);
    }

    boolean isSelectPen = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAutoFillWhiteSpaceZooming) {
            return super.onTouchEvent(event);
        }
        if (state != State.SHOWN) {
            return super.onTouchEvent(event);
        }
        if (readOnlyMode) {
            return dragPinchManager.onTouch(this, event);
        }
        if (annotationManager.areaSelect) {
            processAreaSelectTouch(event);
            return true;
        }
        if (isActiveAreaAnnotation(event)) {
            return annotationManager.onAreaTouch(event, onAreaTouchListener);
        }
        if (function == Function.PEN) {
            pdfViewPenAreaClickCheck(event, new OnPdfViewPenAreaClickListener() {
                @Override
                public void onClick(MotionEvent event, int x, int y) {
                    if (penAnnotations.size() == 0) {
                        return;
                    }
                    boolean isInPenDrawArea = false;
                    PenAnnotation cancelPenAnnotation = null;
                    for (PenAnnotation penAnnotation : penAnnotations) {
                        if (annotationManager.isInCancelArea(penAnnotation, x, y)) {
                            List<BaseAnnotation> thePenAnnotations = new ArrayList<>();
                            thePenAnnotations.add(penAnnotation);
                            annotationManager.removeAnnotations(thePenAnnotations, true);
                            penAnnotation.setAreaRect(null);
                            cancelPenAnnotation = penAnnotation;
                            break;
                        }
                        if (annotationManager.isInPenDrawArea(penAnnotation, x, y)) {
                            isInPenDrawArea = true;
                        }
                    }
                    if (cancelPenAnnotation != null) {
                        penAnnotations.remove(cancelPenAnnotation);
                        redraw();
                        return;
                    }
                    if (!isInPenDrawArea) {
                        for (PenAnnotation penAnnotation : penAnnotations) {
                            penAnnotation.setAreaRect(null);
                        }
                        penAnnotations.clear();
                        redraw();
                    }
                }

                @Override
                public void onLongClick(MotionEvent event, int x, int y) {
                    List<PenAnnotation> thePenAnnotations = annotationManager.getSelectPenAnnotations(x, y, true);
                    if (thePenAnnotations.size() > 0) {//如果选择了新的旧清除旧的
                        for (PenAnnotation penAnnotation : penAnnotations) {
                            if (!thePenAnnotations.contains(penAnnotation)) {
                                penAnnotation.setAreaRect(null);
                            }
                        }
                        penAnnotations.clear();
                        penAnnotations.addAll(thePenAnnotations);
                        annotationManager.drawingPenAnnotation = null;
                        redraw();
                    }
                }
            });
        } else if (function == Function.VIEWER) {
            if (isSelectPen) {
                processPenModeViewer(event, new OnPdfViewProcessClickListener() {
                    @Override
                    public void processClick(MotionEvent event, boolean haveProcess2) {
                    }
                });
            } else {
                processTextModeViewer(event, new OnPdfViewProcessClickListener() {
                    @Override
                    public void processClick(MotionEvent event, boolean haveProcess1) {
                        if (!haveProcess1) {
                            processPenModeViewer(event, new OnPdfViewProcessClickListener() {
                                @Override
                                public void processClick(MotionEvent event, boolean haveProcess2) {
                                    if (!haveProcess2) {
                                        processClickTouch(event);
                                    }
                                }
                            });
                        }
                    }
                });
            }
        }
        switch (function) {
            case VIEWER:
                return dragPinchManager.onTouch(this, event);
            case TEXT:
                return processTextModeTouch(event);
            default:
                return annotationManager.onTouch(event);
        }
    }

    private boolean processClickTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (onPdfViewClickListener != null) {
                    onPdfViewClickListener.onClick(this);
                }
                break;
        }
        return true;
    }

    private boolean processPenModeViewer(MotionEvent event, OnPdfViewProcessClickListener onPdfViewProcessClickListener) {
        if(!enablePenAnnotationClickCheckOnViewMode){
            onPdfViewProcessClickListener.processClick(event, false);
            return false;
        }
        pdfViewTextPenAreaClickCheck(event, new OnPdfViewTextPenAreaClickListener() {
            @Override
            public void onClick(MotionEvent event, int x, int y) {
                if (isSelectPen) {
                    if (penAnnotations.size() == 0) {
                        return;
                    }
                    boolean isInPenDrawArea = false;
                    PenAnnotation cancelPenAnnotation = null;
                    for (PenAnnotation penAnnotation : penAnnotations) {
                        if (annotationManager.isInCancelArea(penAnnotation, x, y)) {
                            List<BaseAnnotation> thePenAnnotations = new ArrayList<>();
                            thePenAnnotations.add(penAnnotation);
                            annotationManager.removeAnnotations(thePenAnnotations, true);
                            penAnnotation.setAreaRect(null);
                            cancelPenAnnotation = penAnnotation;
                            isSelectPen = false;
                            break;
                        }
                        if (annotationManager.isInPenDrawArea(penAnnotation, x, y)) {
                            isInPenDrawArea = true;
                        }
                    }
                    if (cancelPenAnnotation != null) {
                        penAnnotations.remove(cancelPenAnnotation);
                        redraw();
                        onPdfViewProcessClickListener.processClick(event, true);
                        return;
                    }
                    if (!isInPenDrawArea) {
                        for (PenAnnotation penAnnotation : penAnnotations) {
                            penAnnotation.setAreaRect(null);
                        }
                        isSelectPen = false;
                        penAnnotations.clear();
                        redraw();
                    }
                }
                List<PenAnnotation> thePenAnnotations = annotationManager.getSelectPenAnnotations(x, y, true);
                if (thePenAnnotations.size() > 0) {//如果选择了新的旧清除旧的
                    onPdfViewProcessClickListener.processClick(event, true);
                    for (PenAnnotation penAnnotation : penAnnotations) {
                        if (!thePenAnnotations.contains(penAnnotation)) {
                            penAnnotation.setAreaRect(null);
                        }
                    }
                    penAnnotations.clear();
                    penAnnotations.addAll(thePenAnnotations);
                    annotationManager.drawingPenAnnotation = null;
                    isSelectPen = true;
                    redraw();
                } else {
                    onPdfViewProcessClickListener.processClick(event, false);
                }
            }

            @Override
            public void onLongClick(MotionEvent event, int x, int y) {

            }
        });
        return true;
    }

    private boolean processTextModeViewer(MotionEvent event, OnPdfViewProcessClickListener onPdfViewProcessClickListener) {
        if(!enableTextAnnotationClickCheckOnViewMode){
            onPdfViewProcessClickListener.processClick(event, false);
            return false;
        }
        pdfViewTextPenAreaClickCheck(event, new OnPdfViewTextPenAreaClickListener() {
            @Override
            public void onClick(MotionEvent event, int x, int y) {
                if (pdfFile == null) {
                    return;
                }
                List<TextAnnotation> thePenAnnotations = annotationManager.getSelectTextPenAnnotations(x, y, true);
                if (thePenAnnotations.size() > 0) {
                    editTextAnnotation = thePenAnnotations.get(0);
                    RectF rectF = annotationManager.convertPdfPositionToScreenPosition(editTextAnnotation.page, editTextAnnotation.getAreaRect());
                    if (rectF != null) {
                        annotationManager.resetAnnotationDraw(editTextAnnotation);
                        editTextAnnotation.setNeedHidden(true);
                        moveTextRemarkView(rectF.left, rectF.top);
                        onPdfViewProcessClickListener.processClick(event, true);
                        return;
                    }
                }
                onPdfViewProcessClickListener.processClick(event, false);
            }

            @Override
            public void onLongClick(MotionEvent event, int x, int y) {

            }
        });
        return true;
    }

    private boolean processTextModeTouch(MotionEvent event) {
        pdfViewTextPenAreaClickCheck(event, new OnPdfViewTextPenAreaClickListener() {
            @Override
            public void onClick(MotionEvent event, int x, int y) {
                moveTextRemarkView(x, y);
            }

            @Override
            public void onLongClick(MotionEvent event, int x, int y) {
                if (pdfFile == null) {
                    return;
                }
                List<TextAnnotation> thePenAnnotations = annotationManager.getSelectTextPenAnnotations(x, y, true);
                if (thePenAnnotations.size() > 0) {
                    editTextAnnotation = thePenAnnotations.get(0);
                    RectF rectF = annotationManager.convertPdfPositionToScreenPosition(editTextAnnotation.page, editTextAnnotation.getAreaRect());
                    if (rectF != null) {
                        annotationManager.resetAnnotationDraw(editTextAnnotation);
                        editTextAnnotation.setNeedHidden(true);
                        moveTextRemarkView(rectF.left, rectF.top);
                    }
                }
            }
        });
        return true;
    }

    private void processAreaSelectTouch(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        RectF startTargetRect = annotationManager.startTargetRect;
        RectF endTargetRect = annotationManager.endTargetRect;
        if (!annotationManager.areaSelect || startTargetRect == null || endTargetRect == null) {
            return;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (x >= startTargetRect.left && x <= startTargetRect.right && y >= startTargetRect.top && y <= startTargetRect.bottom) {
                    startTargetSelect = true;
                } else if (x >= endTargetRect.left && x <= endTargetRect.right && y >= endTargetRect.top && y <= endTargetRect.bottom) {
                    endTargetSelect = true;
                }
                if (!reTouchStart && (startTargetSelect || endTargetSelect)) {
                    onAreaTouchListener.onReTouchStart();
                    reTouchStart = true;
                }
                isMoveEnd = annotationManager.isMoveEnd(startTargetSelect);
                break;
            case MotionEvent.ACTION_MOVE:
                if (reTouchStart) {
                    annotationManager.onAreaTouchWithReTouch(event, onAreaTouchListener, isMoveEnd);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (reTouchStart) {
                    annotationManager.onAreaTouchWithReTouch(event, onAreaTouchListener, isMoveEnd);
                    onAreaTouchListener.onReTouchComplete();
                } else {
                    onAreaTouchListener.onDismiss();
                }
                reTouchStart = false;
                startTargetSelect = false;
                endTargetSelect = false;
                isMoveEnd = true;
                break;
        }
    }

    /**
     * 检测长按事件,如果是VIEWER的长按则进行拦截并尝试进行选择操作
     */
    private boolean isActiveAreaAnnotation(MotionEvent event) {
        if (function != Function.VIEWER) {
            return false;
        }
        int touchSlop = getTouchSlop();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastMotionX = x;
                lastMotionY = y;
                counter++;
                isReleased = false;
                isMoved = false;
                longClickCheck();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMoved) break;
                if (Math.abs(lastMotionX - x) > touchSlop || Math.abs(lastMotionY - y) > touchSlop) {
                    isMoved = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isReleased = true;
                if (activeAreaAnnotation && !annotationManager.isActiveAreaSelect()) {
                    dismissAreaSelect();
                }
                break;
        }
        return activeAreaAnnotation;
    }

    private int getTouchSlop() {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        return (int) (TOUCH_SLOP * displayMetrics.density);
    }

    private void longClickCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                counter--;
                if (counter > 0 || isReleased || isMoved) {//计数器大于0，说明当前执行的Runnable不是最后一次down产生的
                    return;
                }
                activeAreaAnnotation = true;
                if (onAreaTouchListener != null) {
                    onAreaTouchListener.onActiveArea();
                }
            }
        }, 800);
    }

    private void zoomTextRemarkTextSize() {
        editTextTextRemark.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkCancelTop.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkCancelBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkDeleteTop.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkDeleteBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkSaveTop.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        textViewTextRemarkSaveBottom.setTextSize(TypedValue.COMPLEX_UNIT_PX, editTextRemarkFontSize * pdfFontUnit * zoom);
        int margin = (int) (editTextRemarkFontSize * pdfFontUnit * zoom / 2);
        editTextTextRemark.setPadding(margin, margin, margin, margin);
    }

    private void pdfViewPenAreaClickCheck(MotionEvent event, OnPdfViewPenAreaClickListener onPdfViewPenAreaClickListener) {
        int touchSlop = getTouchSlop();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                penDrawCheckLastMotionX = x;
                penDrawCheckLastMotionY = y;
                penDrawCheckCounter++;
                penDrawCheckIsReleased = false;
                penDrawCheckIsMoved = false;
                penDrawLongClickCheck(event, x, y, onPdfViewPenAreaClickListener);
                penDrawLongClickActive = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (penDrawCheckIsMoved) break;
                if (Math.abs(penDrawCheckLastMotionX - x) > touchSlop || Math.abs(penDrawCheckLastMotionY - y) > touchSlop) {
                    penDrawCheckIsMoved = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                penDrawCheckIsReleased = true;
                if (!penDrawCheckIsMoved && !penDrawLongClickActive) {
                    onPdfViewPenAreaClickListener.onClick(event, x, y);
                }
                break;
        }
    }

    private void penDrawLongClickCheck(MotionEvent event, int x, int y, OnPdfViewPenAreaClickListener onPdfViewPenAreaClickListener) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                penDrawCheckCounter--;
                if (penDrawCheckCounter > 0 || penDrawCheckIsMoved || penDrawCheckIsReleased) {//计数器大于0，说明当前执行的Runnable不是最后一次down产生的
                    return;
                }
                penDrawLongClickActive = true;
                onPdfViewPenAreaClickListener.onLongClick(event, x, y);
            }
        }, 800);
    }


    private boolean pdfViewTextPenAreaClickCheck(MotionEvent event, OnPdfViewTextPenAreaClickListener onPdfViewTextPenAreaClickListener) {
        int touchSlop = getTouchSlop();
        int x = (int) event.getX();
        int y = (int) event.getY();//x,y一定要在longclick之前执行,因为longclik执行回调时候手指已经离开了瞄点的view了,计算的x,y变成了相对于屏幕的,而不是某个view了
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                textPenDrawCheckLastMotionX = x;
                textPenDrawCheckLastMotionY = y;
                textPenDrawCheckCounter++;
                textPenDrawCheckIsReleased = false;
                textPenDrawCheckIsMoved = false;
                textPenDrawLongClickCheck(event, x, y, onPdfViewTextPenAreaClickListener);
                textPenDrawLongClickActive = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (textPenDrawCheckIsMoved) break;
                if (Math.abs(textPenDrawCheckLastMotionX - x) > touchSlop || Math.abs(textPenDrawCheckLastMotionY - y) > touchSlop) {
                    textPenDrawCheckIsMoved = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                textPenDrawCheckIsReleased = true;
                if (!textPenDrawCheckIsMoved && !textPenDrawLongClickActive) {
                    onPdfViewTextPenAreaClickListener.onClick(event, x, y);
                    return true;
                }
                break;
        }
        return false;
    }


    private void textPenDrawLongClickCheck(MotionEvent event, int x, int y, OnPdfViewTextPenAreaClickListener onPdfViewTextPenAreaClickListener) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textPenDrawCheckCounter--;
                if (textPenDrawCheckCounter > 0 || textPenDrawCheckIsMoved || textPenDrawCheckIsReleased) {
                    return;
                }
                textPenDrawLongClickActive = true;
                onPdfViewTextPenAreaClickListener.onLongClick(event, x, y);
            }
        }, 800);
    }


    private void drawWithListener(Canvas canvas, int page, OnDrawListener listener) {
        if (listener != null) {
            float translateX, translateY;
            if (swipeVertical) {
                translateX = 0;
                translateY = pdfFile.getPageOffset(page, zoom);
            } else {
                translateY = 0;
                translateX = pdfFile.getPageOffset(page, zoom);
            }

            canvas.translate(translateX, translateY);
            SizeF size = pdfFile.getPageSize(page);
            listener.onLayerDrawn(canvas,
                    toCurrentScale(size.getWidth()),
                    toCurrentScale(size.getHeight()),
                    page);

            canvas.translate(-translateX, -translateY);
        }
    }

    public boolean isReadOnlyMode() {
        return readOnlyMode;
    }

    public boolean isHaveAnnotation(int page) {
        List<BaseAnnotation> annotations = annotationManager.getAllAnnotation();
        for (int i = 0; i < annotations.size(); i++) {
            if (annotations.get(i).page == page) {
                return true;
            }
        }
        return false;
    }

    public void setReadOnlyMode(boolean readOnlyMode) {
        this.readOnlyMode = readOnlyMode;
    }

    public int getAnnotationRenderingArea() {
        return annotationRenderingArea;
    }

    public void setAnnotationRenderingArea(int annotationRenderingArea) {
        this.annotationRenderingArea = annotationRenderingArea;
    }

    /**
     * Draw a given PagePart on the canvas
     */
    private void drawPart(Canvas canvas, PagePart part) {
        if (recycled || pdfFile == null) {
            return;
        }
        // Can seem strange, but avoid lot of calls
        RectF pageRelativeBounds = part.getPageRelativeBounds();
        Bitmap renderedBitmap = part.getRenderedBitmap();

        if (renderedBitmap.isRecycled()) {
            return;
        }

        // Move to the target page
        float localTranslationX = 0;
        float localTranslationY = 0;
        SizeF size = pdfFile.getPageSize(part.getPage());

        if (swipeVertical) {
            localTranslationY = pdfFile.getPageOffset(part.getPage(), zoom);
            float maxWidth = pdfFile.getMaxPageWidth(getCurrentPage());
            localTranslationX = toCurrentScale(maxWidth - size.getWidth()) / 2;
        } else {
            localTranslationX = pdfFile.getPageOffset(part.getPage(), zoom);
            float maxHeight = pdfFile.getMaxPageHeight(getCurrentPage());
            localTranslationY = toCurrentScale(maxHeight - size.getHeight()) / 2;
        }
        canvas.translate(localTranslationX, localTranslationY);

        Rect srcRect = new Rect(0, 0, renderedBitmap.getWidth(),
                renderedBitmap.getHeight());

        float offsetX = toCurrentScale(pageRelativeBounds.left * size.getWidth());
        float offsetY = toCurrentScale(pageRelativeBounds.top * size.getHeight());
        float width = toCurrentScale(pageRelativeBounds.width() * size.getWidth());
        float height = toCurrentScale(pageRelativeBounds.height() * size.getHeight());

        // If we use float values for this rectangle, there will be
        // a possible gap between page parts, especially when
        // the zoom level is high.
        RectF dstRect = new RectF((int) offsetX, (int) offsetY,
                (int) (offsetX + width),
                (int) (offsetY + height));

        // Check if bitmap is in the screen
        float translationX = currentXOffset + localTranslationX;
        float translationY = currentYOffset + localTranslationY;
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0 ||
                translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0) {
            canvas.translate(-localTranslationX, -localTranslationY);
            return;
        }

        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint);

        if (Constants.DEBUG_MODE) {
            debugPaint.setColor(part.getPage() % 2 == 0 ? Color.RED : Color.BLUE);
            canvas.drawRect(dstRect, debugPaint);
        }

        // Restore the canvas position
        canvas.translate(-localTranslationX, -localTranslationY);

    }

    /**
     * Load all the parts around the center of the screen,
     * taking into account X and Y offsets, zoom level, and
     * the current page displayed
     */
    public void loadPages() {
        if (recycled || renderingHandler == null || pdfFile == null) {
            return;
        }

        // Cancel all current tasks
        renderingHandler.removeMessages(RenderingHandler.MSG_RENDER_TASK);
        cacheManager.makeANewSet();

        pagesLoader.loadPages();
        redraw();
    }

    public void addRenderingCustomTask(RenderingCustomHandler.RenderingCustomTask renderingCustomTask) {
        if (!supportCustomRendering || renderingCustomHandler == null || renderingCustomTask == null) {
            return;
        }
        renderingCustomHandler.addRenderingCustomTask(renderingCustomTask);
    }


    /**
     * Called when the PDF is loaded
     */
    void loadComplete(PdfFile pdfFile) {
        if (recycled || pdfFile == null || renderingHandlerThread == null) {
            return;
        }
        state = State.LOADED;

        this.pdfFile = pdfFile;

        SizeF fristPageSize = pdfFile.getPageSize(0);
        pdfFontUnit = fristPageSize.getWidth() / 360F;

        if (autoFillWhiteSpace) {
            if (!whiteSpaceInfoHandlerThread.isAlive()) {
                whiteSpaceInfoHandlerThread.start();
            }
            whiteSpaceInfoHandler = new WhiteSpaceInfoHandler(whiteSpaceInfoHandlerThread.getLooper(), this);
            whiteSpaceInfoHandler.start();
        }

        if (supportCustomRendering) {
            if (!renderingCustomHandlerThread.isAlive()) {
                renderingCustomHandlerThread.start();
            }
            renderingCustomHandler = new RenderingCustomHandler(renderingCustomHandlerThread.getLooper(), this);
            renderingCustomHandler.start();
        }

        if (autoFillWhiteSpace) {
            List<WhiteSpaceInfoHandler.WhiteSpacePageInfo> pages = new ArrayList<>();
            for (int i = 0; i < pdfFile.getPagesCount(); i++) {
                SizeF size = pdfFile.getPageSize(i);
                pages.add(new WhiteSpaceInfoHandler.WhiteSpacePageInfo(i, size.getWidth(), size.getHeight(), new RectF(0, 0, 1, 1)));
            }
            PDFView.this.whiteSpaceInfos.clear();
            initWhiteSpaceRender = false;
            showWhiteSpaceRenderProgressBar(true);
            whiteSpaceInfoHandler.addWhiteSpaceInfoTask(new WhiteSpaceInfoHandler.WhiteSpaceInfoTask(pages, true, false, new WhiteSpaceInfoHandler.OnWhiteSpaceInfoListener() {
                @Override
                public void onSuccessOne(WhiteSpaceInfo whiteSpaceInfo) {
                    if (!initWhiteSpaceOptimization) {
                        return;
                    }
                    PDFView.this.whiteSpaceInfos.add(whiteSpaceInfo);
                    if (!initWhiteSpaceRender && loadAfterCheckWhiteSpace && (PDFView.this.whiteSpaceInfos.size() >= whiteSpaceRenderPageCountWhenOptimization || PDFView.this.whiteSpaceInfos.size() == pdfFile.getPagesCount())) {
                        initWhiteSpaceRender = true;
                        showWhiteSpaceRenderProgressBar(false);
                        startRendering();
                    }
                }

                @Override
                public void onSuccess(List<WhiteSpaceInfo> whiteSpaceInfos) {
                    if (initWhiteSpaceOptimization) {
                        return;
                    }
                    PDFView.this.whiteSpaceInfos.addAll(whiteSpaceInfos);
                    if (loadAfterCheckWhiteSpace) {
                        showWhiteSpaceRenderProgressBar(false);
                        startRendering();
                    }
                }

                @Override
                public void onError(String error) {
                    showWhiteSpaceRenderProgressBar(false);
                }
            }));
            if (!loadAfterCheckWhiteSpace) {
                startRendering();
            }
        } else {
            startRendering();
        }
    }

    void showWhiteSpaceRenderProgressBar(boolean isShow) {
        if (!showLoadingWhenWhiteSpaceRender) {
            return;
        }
        if (isShow) {
            frameLayoutProgressBar.setVisibility(VISIBLE);
        } else {
            frameLayoutProgressBar.setVisibility(GONE);
        }
    }

    void startRendering() {
        if (recycled || pdfFile == null || renderingHandlerThread == null) {
            return;
        }
        if (!renderingHandlerThread.isAlive()) {
            renderingHandlerThread.start();
        }
        renderingHandler = new RenderingHandler(renderingHandlerThread.getLooper(), this);
        renderingHandler.start();

        if (scrollHandle != null) {
            scrollHandle.setupLayout(this);
            isScrollHandleInit = true;
        }

        dragPinchManager.enable();

        callbacks.callOnLoadComplete(pdfFile.getPagesCount());
        jumpTo(defaultPage, false);
    }

    float getWhiteSpaceZoom(int page) {
        float zoom = 1;
        if (useMinWhiteSpaceZoom) {
            for (WhiteSpaceInfo whiteSpaceInfo : whiteSpaceInfos) {
                if (zoom == 1 || zoom > whiteSpaceInfo.getScale()) {
                    zoom = whiteSpaceInfo.getScale();
                }
            }
            return zoom;
        }
        for (WhiteSpaceInfo whiteSpaceInfo : whiteSpaceInfos) {
            if (whiteSpaceInfo.getPage() == page) {
                return whiteSpaceInfo.getScale();
            }
        }
        return zoom;
    }

    WhiteSpaceInfo getWhiteSpaceInfo(int page) {
        for (WhiteSpaceInfo whiteSpaceInfo : whiteSpaceInfos) {
            if (whiteSpaceInfo.getPage() == page) {
                return whiteSpaceInfo;
            }
        }
        return null;
    }


    void loadError(Throwable t) {
        state = State.ERROR;
        // store reference, because callbacks will be cleared in recycle() method
        OnErrorListener onErrorListener = callbacks.getOnError();
        recycle();
        redraw();
        if (onErrorListener != null) {
            onErrorListener.onError(t);
        } else {
            Log.e("PDFView", "load pdf error", t);
        }
    }

    void redraw() {
        invalidate();
        customRenderingView.invalidate();
        redrawRenderingView();
        redrawPenDrawingView();
    }

    void redrawRenderingView() {
        customRenderingView.invalidate();
    }

    void redrawPenDrawingView() {
        customPenHaveDrawingView.invalidate();
        customPenDrawingView.invalidate();
    }

    /**
     * Called when a rendering task is over and
     * a PagePart has been freshly created.
     *
     * @param part The created PagePart.
     */
    public void onBitmapRendered(PagePart part) {
        if (recycled || pdfFile == null) {
            return;
        }
        // when it is first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN;
            callbacks.callOnRender(pdfFile.getPagesCount());
        }

        if (part.isThumbnail()) {
            cacheManager.cacheThumbnail(part);
        } else {
            cacheManager.cachePart(part);
        }
        redraw();
    }

    public void moveTo(float offsetX, float offsetY) {
        moveTo(offsetX, offsetY, true);
    }

    /**
     * Move to the given X and Y offsets, but check them ahead of time
     * to be sure not to go outside the the big strip.
     *
     * @param offsetX    The big strip X offset to use as the left border of the screen.
     * @param offsetY    The big strip Y offset to use as the right border of the screen.
     * @param moveHandle whether to move scroll handle or not
     */
    public void moveTo(float offsetX, float offsetY, boolean moveHandle) {
        if (pdfFile == null) {
            return;
        }
        if (recycled || swipeVertical) {
            // Check X offset
            float scaledPageWidth = toCurrentScale(pdfFile.getMaxPageWidth(getCurrentPage()));
            if (scaledPageWidth < getWidth()) {
                offsetX = getWidth() / 2 - scaledPageWidth / 2;
            } else {
                if (offsetX > 0) {
                    offsetX = 0;
                } else if (offsetX + scaledPageWidth < getWidth()) {
                    offsetX = getWidth() - scaledPageWidth;
                }
            }

            // Check Y offset
            float contentHeight = pdfFile.getDocLen(zoom);
            if (contentHeight < getHeight()) { // whole document height visible on screen
                offsetY = (getHeight() - contentHeight) / 2;
            } else {
                if (offsetY > 0) { // top visible
                    offsetY = 0;
                } else if (offsetY + contentHeight < getHeight()) { // bottom visible
                    offsetY = -contentHeight + getHeight();
                }
            }

            if (offsetY < currentYOffset) {
                scrollDir = ScrollDir.END;
            } else if (offsetY > currentYOffset) {
                scrollDir = ScrollDir.START;
            } else {
                scrollDir = ScrollDir.NONE;
            }
        } else {
            // Check Y offset
            float scaledPageHeight = toCurrentScale(pdfFile.getMaxPageHeight(getCurrentPage()));
            if (scaledPageHeight < getHeight()) {
                offsetY = getHeight() / 2 - scaledPageHeight / 2;
            } else {
                if (offsetY > 0) {
                    offsetY = 0;
                } else if (offsetY + scaledPageHeight < getHeight()) {
                    offsetY = getHeight() - scaledPageHeight;
                }
            }

            // Check X offset
            float contentWidth = pdfFile.getDocLen(zoom);
            if (contentWidth < getWidth()) { // whole document width visible on screen
                offsetX = (getWidth() - contentWidth) / 2;
            } else {
                if (offsetX > 0) { // left visible
                    offsetX = 0;
                } else if (offsetX + contentWidth < getWidth()) { // right visible
                    offsetX = -contentWidth + getWidth();
                }
            }

            if (offsetX < currentXOffset) {
                scrollDir = ScrollDir.END;
            } else if (offsetX > currentXOffset) {
                scrollDir = ScrollDir.START;
            } else {
                scrollDir = ScrollDir.NONE;
            }
        }

        currentXOffset = offsetX;
        currentYOffset = offsetY;
        float positionOffset = getPositionOffset();

        if (moveHandle && scrollHandle != null && !documentFitsView()) {
            scrollHandle.setScroll(positionOffset);
        }

        callbacks.callOnPageScroll(getCurrentPage(), positionOffset);
        redraw();
    }

    void loadPageByOffset() {
        if (recycled || pdfFile == null || 0 == pdfFile.getPagesCount()) {
            return;
        }

        float offset, screenCenter;
        if (swipeVertical) {
            offset = currentYOffset;
            screenCenter = ((float) getHeight()) / 2;
        } else {
            offset = currentXOffset;
            screenCenter = ((float) getWidth()) / 2;
        }

        int page = pdfFile.getPageAtOffset(-(offset - screenCenter), zoom);

        if (page >= 0 && page <= pdfFile.getPagesCount() - 1 && page != getCurrentPage()) {
            showPage(page, true);
        } else {
            loadPages();
        }
    }

    void loadPageByOffsetWithAutoFillCheck() {
        if (recycled || pdfFile == null || 0 == pdfFile.getPagesCount()) {
            return;
        }

        float offset, screenCenter;
        if (swipeVertical) {
            offset = currentYOffset;
            screenCenter = ((float) getHeight()) / 2;
        } else {
            offset = currentXOffset;
            screenCenter = ((float) getWidth()) / 2;
        }

        int page = pdfFile.getPageAtOffset(-(offset - screenCenter), zoom);

        if (page >= 0 && page <= pdfFile.getPagesCount() - 1 && page != getCurrentPage()) {
            showPage(page, false);
            jumpToPageWithAutoFillCheck(getCurrentPage());
        } else {
            loadPages();
        }
    }

    /**
     * Animate to the nearest snapping position for the current SnapPolicy
     */
    public void performPageSnap() {
        if (recycled || pdfFile == null || !pageSnap || pdfFile.getPagesCount() == 0) {
            return;
        }
        int centerPage = findFocusPage(currentXOffset, currentYOffset);
        SnapEdge edge = findSnapEdge(centerPage);
        if (edge == SnapEdge.NONE) {
            return;
        }
        float offset = snapOffsetForPage(centerPage, edge);
        if (swipeVertical) {
            animationManager.startYAnimation(currentYOffset, -offset);
        } else {
            animationManager.startXAnimation(currentXOffset, -offset);
        }
    }

    public void whiteSpaceZoomCheck(int page, Runnable runnable) {
        if (recycled || pdfFile == null || !pageSnap || pdfFile.getPagesCount() == 0 || runnable == null) {
            return;
        }
        boolean needZoom = false;
        try {
            float theZoom = getWhiteSpaceZoom(page);
            if (zoom != theZoom) {
                needZoom = true;
                zoomWithAnimation(theZoom);
            }
        } finally {
            if (needZoom) {
                autoFillWhiteSpaceHandler.postDelayed(runnable, 500);//animationManager会清理旧动画,等待缩放执行完
            } else {
                runnable.run();
            }
        }
    }

    public void performPageSnapToCenter(int page, boolean whiteSpaceZoom) {
        if (recycled || pdfFile == null || !pageSnap || pdfFile.getPagesCount() == 0) {
            return;
        }
        if (whiteSpaceZoom) {
            whiteSpaceZoomCheck(page, new Runnable() {
                @Override
                public void run() {
                    snapToCenter();
                }
            });
        } else {
            snapToCenter();
        }
    }

    public void snapToTop() {
        if (recycled || pdfFile == null) {
            return;
        }
        moveTo(currentXOffset,0);
    }

    public void addAnimationEndRunnable(String key, Runnable runnable) {
        if (!isDoZooming() && !isDoMoving() && !isDoFlinging()) {
            runnable.run();
            return;
        }
        animationEndRunnables.put(key, runnable);
    }

    public void jumpToPageWithAutoFillCheck(int page) {
        if (recycled || pdfFile == null) {
            return;
        }
        SnapEdge edge = isAutoFillWhiteSpace() ? SnapEdge.CENTER : findSnapEdge(page);
        float offset = snapOffsetForPage(page, edge);
        animationManager.startPageFlingAnimation(-offset);
    }

    public void snapToCenter() {
        if (recycled || pdfFile == null) {
            return;
        }
        int centerPage = findFocusPage(currentXOffset, currentYOffset);
        SnapEdge edge = SnapEdge.CENTER;
        float offset = snapOffsetForPage(centerPage, edge);
        if (swipeVertical) {
            animationManager.startYAnimation(currentYOffset, -offset);
        } else {
            animationManager.startXAnimation(currentXOffset, -offset);
        }
    }

    public Bitmap getRenderingBitmap(int page, int targetWidth) {
        return customRenderingView.getRenderingBitmap(page,targetWidth);
    }

    public RenderedBitmap getRenderingBitmapWithBase64(int page, int targetWidth) {
        Bitmap bitmap = getRenderingBitmap(page,targetWidth);
        if (bitmap == null) {
            return null;
        }
        Size pdfSize = pdfFile.originalPageSizes.get(page);
        RenderedBitmap renderedBitmap = new RenderedBitmap(bitmap.getWidth(), bitmap.getHeight(),pdfSize.getWidth(),pdfSize.getHeight(), Base64Util.bitmapToBase64(bitmap, true, targetWidth));
        return renderedBitmap;
    }

    /**
     * Find the edge to snap to when showing the specified page
     */
    SnapEdge findSnapEdge(int page) {
        if (!pageSnap || page < 0 || recycled || pdfFile == null) {
            return SnapEdge.NONE;
        }
        float currentOffset = swipeVertical ? currentYOffset : currentXOffset;
        float offset = -pdfFile.getPageOffset(page, zoom);
        int length = swipeVertical ? getHeight() : getWidth();
        float pageLength = pdfFile.getPageLength(page, zoom);

        if (length >= pageLength) {
            return SnapEdge.CENTER;
        } else if (currentOffset >= offset) {
            return SnapEdge.START;
        } else if (offset - pageLength > currentOffset - length) {
            return SnapEdge.END;
        } else {
            return SnapEdge.NONE;
        }
    }

    /**
     * Get the offset to move to in order to snap to the page
     */
    float snapOffsetForPage(int pageIndex, SnapEdge edge) {
        if (recycled || pdfFile == null) {
            return 0;
        }
        float offset = pdfFile.getPageOffset(pageIndex, zoom);

        float length = swipeVertical ? getHeight() : getWidth();
        float pageLength = pdfFile.getPageLength(pageIndex, zoom);

        if (edge == SnapEdge.CENTER) {
            offset = offset - length / 2f + pageLength / 2f;
        } else if (edge == SnapEdge.END) {
            offset = offset - length + pageLength;
        }
        return offset;
    }

    int findFocusPage(float xOffset, float yOffset) {
        if (recycled || pdfFile == null) {
            return 0;
        }
        float currOffset = swipeVertical ? yOffset : xOffset;
        float length = swipeVertical ? getHeight() : getWidth();
        // make sure first and last page can be found
        if (currOffset > -1) {
            return 0;
        } else if (currOffset < -pdfFile.getDocLen(zoom) + length + 1) {
            return pdfFile.getPagesCount() - 1;
        }
        // else find page in center
        float center = currOffset - length / 2f;
        return pdfFile.getPageAtOffset(-center, zoom);
    }

    /**
     * @return true if single page fills the entire screen in the scrolling direction
     */
    public boolean pageFillsScreen() {
        if (recycled || pdfFile == null) {
            return false;
        }
        float start = -pdfFile.getPageOffset(currentPage, zoom);
        float end = start - pdfFile.getPageLength(currentPage, zoom);
        if (isSwipeVertical()) {
            return start > currentYOffset && end < currentYOffset - getHeight();
        } else {
            return start > currentXOffset && end < currentXOffset - getWidth();
        }
    }

    public boolean pageFillsHorizontalScreen() {
        if (recycled || pdfFile == null) {
            return false;
        }
        float start = -pdfFile.getPageOffset(currentPage, zoom);
        float end = start - pdfFile.getPageLength(currentPage, zoom);
        return start > currentXOffset && end < currentXOffset - getWidth();
    }

    public boolean isFirstPage() {
        if (currentPage == 0) {
            return true;
        }
        return false;
    }

    public boolean isLastPage() {
        if (currentPage == getPageCount() - 1) {
            return true;
        }
        return false;
    }

    public float getDpi(){
        if(pdfFile==null){
            return 1;
        }
        return pdfFile.getDpi();
    }

    /**
     * Move relatively to the current position.
     *
     * @param dx The X difference you want to apply.
     * @param dy The Y difference you want to apply.
     * @see #moveTo(float, float)
     */
    public void moveRelativeTo(float dx, float dy) {
        moveTo(currentXOffset + dx, currentYOffset + dy);
    }

    private boolean isZoomNear(WhiteSpaceInfo whiteSpaceInfo) {
        if(whiteSpaceInfo==null){
            return false;
        }
        return whiteSpaceInfo.getScale() < zoom * 1.01 && whiteSpaceInfo.getScale() > zoom * 0.99;
    }

    public boolean checkWhiteSpaceTouchLimit() {
        if (recycled || pdfFile == null) {
            return false;
        }
        int centerPage = findFocusPage(currentXOffset, currentYOffset);
        WhiteSpaceInfo whiteSpaceInfo = getWhiteSpaceInfo(centerPage);
        if ((autoFillWhiteSpace && isZoomNear(whiteSpaceInfo)) || isDoZooming() || isDoMoving() || isDoFlinging()) {
            return true;
        }
        return false;
    }

    public boolean moveRelativeToWithWhiteSpaceTouchLimitCheck(float dx, float dy) {
        if (recycled || pdfFile == null) {
            return false;
        }
        int centerPage = findFocusPage(currentXOffset, currentYOffset);
        WhiteSpaceInfo whiteSpaceInfo = getWhiteSpaceInfo(centerPage);
        if ((autoFillWhiteSpace && isZoomNear(whiteSpaceInfo)) || isDoZooming() || isDoMoving() || isDoFlinging()) {
            if (swipeVertical) {
                moveTo(currentXOffset + dx, currentYOffset);
            } else {
                moveTo(currentXOffset, currentYOffset + dy);
            }
            return true;
        }
        moveTo(currentXOffset + dx, currentYOffset + dy);
        return false;
    }

    /**
     * Change the zoom level
     */
    public void zoomTo(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Change the zoom level, relatively to a pivot point.
     * It will call moveTo() to make sure the given point stays
     * in the middle of the screen.
     *
     * @param zoom  The zoom level.
     * @param pivot The point on the screen that should stays.
     */
    public void zoomCenteredTo(float zoom, PointF pivot) {
        float dzoom = zoom / this.zoom;
        zoomTo(zoom);
        float baseX = currentXOffset * dzoom;
        float baseY = currentYOffset * dzoom;
        baseX += (pivot.x - pivot.x * dzoom);
        baseY += (pivot.y - pivot.y * dzoom);
        moveTo(baseX, baseY);
    }

    /**
     * @see #zoomCenteredTo(float, PointF)
     */
    public void zoomCenteredRelativeTo(float dzoom, PointF pivot) {
        zoomCenteredTo(zoom * dzoom, pivot);
    }

    public void zoomAnimationStart() {
        doZooming = true;
    }

    public void zoomAnimationEnd() {
        doZooming = false;
        checkAllAnimationEnd();
    }

    public void moveAnimationStart() {
        doMoving = true;
    }

    public void moveAnimationEnd() {
        doMoving = false;
        checkAllAnimationEnd();
    }

    public void flingingEnd() {
        checkAllAnimationEnd();
    }

    private void checkAllAnimationEnd() {
        if (isDoZooming() || isDoMoving() || isDoFlinging()) {
            return;
        }
        if (animationEndRunnables.size() == 0) {
            return;
        }
        if (animationEndRunnables.containsKey(START_ZOOM_ANIMATION)) {
            Runnable runnable = animationEndRunnables.get(START_ZOOM_ANIMATION);
            animationEndRunnables.remove(START_ZOOM_ANIMATION);
            if (runnable != null) {
                runnable.run();
            }
            return;
        }
        List<Runnable> runnables = new ArrayList<>();
        for (Map.Entry<String, Runnable> entry : animationEndRunnables.entrySet()) {
            Runnable runnable = entry.getValue();
            if (runnable != null) {
                runnables.add(runnable);
            }
        }
        animationEndRunnables.clear();//执行runnable之前需要清理,以免runnable内执行动画相关方法导致循环调用
        for (Runnable runnable : runnables) {
            runnable.run();
        }
    }

    public boolean isDoZooming() {
        return doZooming;
    }

    public boolean isDoMoving() {
        return doMoving;
    }

    public boolean isDoFlinging() {
        return animationManager.isFlinging();
    }


    /**
     * Checks if whole document can be displayed on screen, doesn't include zoom
     *
     * @return true if whole document can displayed at once, false otherwise
     */
    public boolean documentFitsView() {
        if (recycled || pdfFile == null) {
            return false;
        }
        float len = pdfFile.getDocLen(1);
        if (swipeVertical) {
            return len < getHeight();
        } else {
            return len < getWidth();
        }
    }

    public void fitToWidth(int page) {
        if (recycled || pdfFile == null || state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet");
            return;
        }
        zoomTo(getWidth() / pdfFile.getPageSize(page).getWidth());
        jumpTo(page);
    }

    public SizeF getPageSize(int pageIndex) {
        if (recycled || pdfFile == null) {
            return new SizeF(0, 0);
        }
        return pdfFile.getPageSize(pageIndex);
    }

    /**
     * 获取pdf页面于display页面的比例
     */
    public float getPdfPageScale(int pageIndex) {
        if (recycled || pdfFile == null) {
            return 0;
        }
        return pdfFile.originalPageSizes.get(pageIndex).getWidth() / pdfFile.getPageSize(pageIndex).getWidth();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public float getCurrentXOffset() {
        return currentXOffset;
    }

    public float getCurrentYOffset() {
        return currentYOffset;
    }

    public float toRealScale(float size) {
        return size / zoom;
    }

    public float toCurrentScale(float size) {
        return size * zoom;
    }

    public float getZoom() {
        return zoom;
    }

    public boolean isZooming() {
        return zoom != minZoom;
    }

    private void setDefaultPage(int defaultPage) {
        this.defaultPage = defaultPage;
    }

    public void resetZoom() {
        zoomTo(defaultZoom);
    }

    public void resetZoomWithAnimation() {
        if (autoFillWhiteSpace) {
            zoomWithAnimation(getWhiteSpaceZoom(currentPage));
            autoFillWhiteSpaceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    snapToCenter();
                }
            }, 500);
        } else {
            zoomWithAnimation(minZoom);
        }
    }

    public void zoomWithAnimation(float centerX, float centerY, float scale) {
        addAnimationEndRunnable(START_ZOOM_ANIMATION, new Runnable() {
            @Override
            public void run() {
                animationManager.startZoomAnimation(centerX, centerY, zoom, scale);
            }
        });
    }

    public void zoomWithAnimation(float scale) {
        addAnimationEndRunnable(START_ZOOM_ANIMATION, new Runnable() {
            @Override
            public void run() {
                animationManager.startZoomAnimation(getWidth() / 2, getHeight() / 2, zoom, scale);
            }
        });
    }

    private void setScrollHandle(ScrollHandle scrollHandle) {
        this.scrollHandle = scrollHandle;
    }

    /**
     * Get page number at given offset
     *
     * @param positionOffset scroll offset between 0 and 1
     * @return page number at given offset, starting from 0
     */
    public int getPageAtPositionOffset(float positionOffset) {
        if (recycled || pdfFile == null) {
            return 0;
        }
        return pdfFile.getPageAtOffset(pdfFile.getDocLen(zoom) * positionOffset, zoom);
    }

    public float getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom;
    }

    public float getMidZoom() {
        return midZoom;
    }

    public void setMidZoom(float midZoom) {
        this.midZoom = midZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public float getDefaultZoom() {
        return defaultZoom ;
    }

    public void setDefaultZoom(float defaultZoom) {
        this.defaultZoom = defaultZoom;
    }

    public void useBestQuality(boolean bestQuality) {
        this.bestQuality = bestQuality;
    }

    public boolean isBestQuality() {
        return bestQuality;
    }

    public boolean isSwipeVertical() {
        return swipeVertical;
    }

    public boolean isSwipeEnabled() {
        return enableSwipe;
    }

    private void setSwipeVertical(boolean swipeVertical) {
        this.swipeVertical = swipeVertical;
    }

    public void enableAnnotationRendering(boolean annotationRendering) {
        this.annotationRendering = annotationRendering;
    }

    public boolean isAnnotationRendering() {
        return annotationRendering;
    }

    public void enableRenderDuringScale(boolean renderDuringScale) {
        this.renderDuringScale = renderDuringScale;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public boolean isAntialiasing() {
        return enableAntialiasing;
    }

    public void enableAntialiasing(boolean enableAntialiasing) {
        this.enableAntialiasing = enableAntialiasing;
    }

    public PaintFlagsDrawFilter getAntialiasFilter() {
        return antialiasFilter;
    }

    public void setAntialiasFilter(PaintFlagsDrawFilter antialiasFilter) {
        this.antialiasFilter = antialiasFilter;
    }

    public int getSpacingPx() {
        return spacingPx;
    }

    public boolean isAutoSpacingEnabled() {
        return autoSpacing;
    }

    public boolean isDrawingPenOptimizeEnabled() {
        return drawingPenOptimize;
    }

    public void setPageFling(boolean pageFling) {
        this.pageFling = pageFling;
    }

    public boolean isPageFlingEnabled() {
        return pageFling;
    }

    private void setSpacing(int spacingDp) {
        this.spacingPx = Util.getDP(getContext(), spacingDp);
    }

    private void setAutoSpacing(boolean autoSpacing) {
        this.autoSpacing = autoSpacing;
    }

    private void setDrawingPenOptimize(boolean drawingPenOptimize) {
        this.drawingPenOptimize = drawingPenOptimize;
    }

    private void setPageFitPolicy(FitPolicy pageFitPolicy) {
        this.pageFitPolicy = pageFitPolicy;
    }

    public FitPolicy getPageFitPolicy() {
        return pageFitPolicy;
    }

    private void setFitEachPage(boolean fitEachPage) {
        this.fitEachPage = fitEachPage;
    }

    public boolean isFitEachPage() {
        return fitEachPage;
    }

    public boolean isPageSnap() {
        return pageSnap;
    }

    public void setPageSnap(boolean pageSnap) {
        this.pageSnap = pageSnap;
    }

    public boolean doRenderDuringScale() {
        return renderDuringScale;
    }

    public Function getFunction() {
        return function;
    }

    private void setFunction(Function function) {
        this.function = function;
    }

    public void setPen(@NonNull Pen.WritePen pen) {
        annotationManager.setPen(pen);
    }

    public void setPenMode(@NonNull Pen.WritePen pen) {
        setFunction(Function.PEN);
        annotationManager.setPen(pen);
        resetPenArea();
    }

    public void resetPenArea() {
        for (PenAnnotation penAnnotation : penAnnotations) {
            penAnnotation.setAreaRect(null);
        }
        isSelectPen = false;
        redraw();
    }

    public void setTextMode(@NonNull TextPen textPen) {
        setFunction(Function.TEXT);
        annotationManager.setTextPen(textPen);
        resetPenArea();

        setEditTextNormalColor(textPen.getColor());
        updateEditTextRemarkView();
        if (textPen.getFontSize() != 0) {
            setEditTextRemarkFontSize(textPen.getFontSize());
            zoomTextRemarkTextSize();
//            float[] position = new float[3];
//            if (linearLayoutTextRemarkContentView.getTag() != null) {
//                position = (float[]) linearLayoutTextRemarkContentView.getTag();
//                if (position[0] != 0 && position[1] != 0) {
//                    moveTextRemarkView((int) (position[0]), (int) (position[1]));
//                }
//            }
        }
    }

    public void setTextPen(@NonNull TextPen textPen) {
        annotationManager.setTextPen(textPen);
        setEditTextNormalColor(textPen.getColor());
        updateEditTextRemarkView();
        if (textPen.getFontSize() != 0) {
            setEditTextRemarkFontSize(textPen.getFontSize());
            zoomTextRemarkTextSize();
            float[] position = new float[3];
            if (linearLayoutTextRemarkContentView.getTag() != null) {
                position = (float[]) linearLayoutTextRemarkContentView.getTag();
                if (position[0] != 0 && position[1] != 0) {
                    moveTextRemarkView((int) (position[0]), (int) (position[1]));
                }
            }
        }
    }

    public boolean isAutoFillWhiteSpace() {
        return autoFillWhiteSpace;
    }

    public void setAutoFillWhiteSpace(boolean autoFillWhiteSpace) {
        this.autoFillWhiteSpace = autoFillWhiteSpace;
    }

    public boolean isSupportCustomRendering() {
        return supportCustomRendering;
    }

    public void setSupportCustomRendering(boolean supportCustomRendering) {
        this.supportCustomRendering = supportCustomRendering;
    }

    public boolean isLoadAfterCheckWhiteSpace() {
        return loadAfterCheckWhiteSpace;
    }

    public void setLoadAfterCheckWhiteSpace(boolean loadAfterCheckWhiteSpace) {
        this.loadAfterCheckWhiteSpace = loadAfterCheckWhiteSpace;
    }

    public boolean isTouchWithoutSpace() {
        return touchWithoutSpace;
    }

    public void setTouchWithoutSpace(boolean touchWithoutSpace) {
        this.touchWithoutSpace = touchWithoutSpace;
    }

    public boolean isUseMinWhiteSpaceZoom() {
        return useMinWhiteSpaceZoom;
    }

    public void setUseMinWhiteSpaceZoom(boolean useMinWhiteSpaceZoom) {
        this.useMinWhiteSpaceZoom = useMinWhiteSpaceZoom;
    }

    public boolean isInitWhiteSpaceOptimization() {
        return initWhiteSpaceOptimization;
    }

    public void setInitWhiteSpaceOptimization(boolean initWhiteSpaceOptimization) {
        this.initWhiteSpaceOptimization = initWhiteSpaceOptimization;
    }

    public boolean isWhiteSpaceRenderBestQuality() {
        return isWhiteSpaceRenderBestQuality;
    }

    public void setWhiteSpaceRenderBestQuality(boolean whiteSpaceRenderBestQuality) {
        isWhiteSpaceRenderBestQuality = whiteSpaceRenderBestQuality;
    }

    public float getWhiteSpaceRenderThumbnailRatio() {
        return whiteSpaceRenderThumbnailRatio;
    }

    public void setWhiteSpaceRenderThumbnailRatio(float whiteSpaceRenderThumbnailRatio) {
        this.whiteSpaceRenderThumbnailRatio = whiteSpaceRenderThumbnailRatio;
    }

    public int getWhiteSpaceRenderPageCountWhenOptimization() {
        return whiteSpaceRenderPageCountWhenOptimization;
    }

    public void setWhiteSpaceRenderPageCountWhenOptimization(int whiteSpaceRenderPageCountWhenOptimization) {
        this.whiteSpaceRenderPageCountWhenOptimization = whiteSpaceRenderPageCountWhenOptimization;
    }

    public Bitmap getCancelBitmap() {
        return cancelBitmap;
    }

    public void setCancelBitmap(Bitmap cancelBitmap) {
        this.cancelBitmap = cancelBitmap;
    }

    public float getCancelBitmapSize() {
        return cancelBitmapSize;
    }

    public void setCancelBitmapSize(float cancelBitmapSize) {
        this.cancelBitmapSize = cancelBitmapSize;
    }

    public float getEditTextRemarkFontSize() {
        return editTextRemarkFontSize;
    }

    public void setEditTextRemarkFontSize(float editTextRemarkFontSize) {
        this.editTextRemarkFontSize = editTextRemarkFontSize;
    }

    public int getEditTextRemarkThemeColor() {
        return editTextRemarkThemeColor;
    }

    public void setEditTextRemarkThemeColor(int editTextRemarkThemeColor) {
        this.editTextRemarkThemeColor = editTextRemarkThemeColor;
    }

    public int getEditTextNormalColor() {
        return editTextNormalColor;
    }

    public void setEditTextNormalColor(int editTextNormalColor) {
        this.editTextNormalColor = editTextNormalColor;
    }

    public int getEditTextHintColor() {
        return editTextHintColor;
    }

    public void setEditTextHintColor(int editTextHintColor) {
        this.editTextHintColor = editTextHintColor;
    }

    public boolean isShowLoadingWhenWhiteSpaceRender() {
        return showLoadingWhenWhiteSpaceRender;
    }

    public void setShowLoadingWhenWhiteSpaceRender(boolean showLoadingWhenWhiteSpaceRender) {
        this.showLoadingWhenWhiteSpaceRender = showLoadingWhenWhiteSpaceRender;
    }

    public void setViewerMode() {
        setFunction(Function.VIEWER);
        if (penAnnotations.size() > 0) {
            for (PenAnnotation penAnnotation : penAnnotations) {
                penAnnotation.setAreaRect(null);
            }
            penAnnotations.clear();
            redraw();
        }
        resetEditTextAnnotation();
        if (relativeLayoutTextRemarkView != null && relativeLayoutTextRemarkView.getVisibility() == VISIBLE) {
            relativeLayoutTextRemarkView.setVisibility(GONE);
        }
    }
    public void setAreaPen(@NonNull AreaPen pen) {
        annotationManager.setAreaPen(pen);
    }

    public void setSearchAreaPen(@NonNull SearchAreaPen pen) {
        annotationManager.setSearchAreaPen(pen);
    }

    public void setEraserMode(@NonNull Eraser eraser) {
        setFunction(Function.ERASER);
        annotationManager.setEraser(eraser);
    }

    public void setMarkMode(@NonNull Pen.MarkPen pen) {
        setFunction(Function.MARK);
        annotationManager.setMarkPen(pen);
    }

    public boolean isEnablePenAnnotationClickCheckOnViewMode() {
        return enablePenAnnotationClickCheckOnViewMode;
    }

    public void setEnablePenAnnotationClickCheckOnViewMode(boolean enable) {
        enablePenAnnotationClickCheckOnViewMode = enable;
    }

    public boolean isEnableTextAnnotationClickCheckOnViewMode() {
        return enableTextAnnotationClickCheckOnViewMode;
    }

    public void setEnableTextAnnotationClickCheckOnViewMode(boolean enable) {
        enableTextAnnotationClickCheckOnViewMode = enable;
    }

    public void dismissAreaSelect() {
        lastMotionX = 0;
        lastMotionY = 0;
        activeAreaAnnotation = false;
        annotationManager.clearArea();
    }

    public void savePenDrawing() {
        annotationManager.savePenDrawing();
    }

    public void cancelPenDrawing() {
        annotationManager.cancelPenDrawing();
    }

    public void clearPenDrawing() {
        annotationManager.clearPenDrawing();
    }

    public void drawSearchArea(SearchTextInfo searchTextInfo) {
        annotationManager.drawSearchArea(searchTextInfo);
    }

    public void clearSearchArea() {
        annotationManager.clearSearchArea();
    }

    public void setDrawAreaPen(MarkAreaType markAreaType, Pen.MarkPen markPen) {
        if (markAreaType == null) {
            return;
        }
        annotationManager.setAreaMarkPen(markAreaType, markPen);
    }

    public boolean drawSelectAreaWithMarkAreaType(MarkAreaType markAreaType) {
        if (markAreaType == null) {
            return false;
        }
        switch (markAreaType) {
            case DELETELINE:
            case UNDERLINE:
            case UNDERWAVELINE:
            case HIGHLIGHT:
                return annotationManager.drawSelectAreaWithMarkAreaType(markAreaType);
            default:
                break;
        }
        return false;
    }

    public boolean cancelSelectAreaAnnotation(MarkAreaType markAreaType) {
        if (markAreaType == null) {
            return false;
        }
        switch (markAreaType) {
            case DELETELINE:
            case UNDERLINE:
            case UNDERWAVELINE:
            case HIGHLIGHT:
                return annotationManager.cancelSelectAreaAnnotation(markAreaType);
            default:
                break;
        }
        return false;
    }

    public void clearPage() {
        annotationManager.clearPage();
    }

    public String searchTextByIndex(int index) {
        if (recycled || pdfFile == null || pdfFile.pdfDocument == null) {
            return null;
        }
        Long pagePtr = pdfFile.pdfDocument.getTextPagesPtr(getCurrentPage());
        if (pagePtr == null) {
            return null;
        }
        int result = pdfiumCore.searchTextUnicode(pagePtr, index);
        return UnicodeUtil.unicodeToString(UnicodeUtil.convertUnicode(result));
    }

    public boolean isSingleZoom() {
        return singleZoom;
    }

    public void setSingleZoom(boolean singleZoom) {
        this.singleZoom = singleZoom;
    }

    /**
     * searchType==1 表示搜索当前页
     * searchType==2 表示搜索所有页,返回匹配的第一页数据
     * searchType==3 表示搜索所有页,优先返回本页
     */
    public void searchText(String text, int searchType, OnSearchTextListener onSearchTextListener) {
        if (recycled || pdfFile == null || pdfFile.pdfDocument == null) {
            return;
        }
        int pageSize = getPageCount();
        int currentPage = getCurrentPage();
        List<Integer> pageIndexs = new ArrayList<>();
        List<Long> textPagesPtrs = new ArrayList<>();

        if (pdfFile.pdfiumCore != null && pdfFile.pdfDocument.getNativeTextPagesPtrSize() != pageSize) {
            pdfFile.pdfiumCore.openPage(pdfFile.pdfDocument, 0, pageSize - 1);
        }
        if (searchType == 1) {
            pageIndexs.add(currentPage);
            Long textPagesPtr = pdfFile.pdfDocument.getTextPagesPtr(currentPage);
            if (textPagesPtr == null) {
                return;
            }
            textPagesPtrs.add(textPagesPtr);
        } else if (searchType == 2 || searchType == 3) {
            for (int i = 0; i < pageSize; i++) {
                int pageIndex = i;
                Long textPagesPtr = pdfFile.pdfDocument.getTextPagesPtr(pageIndex);
                if (textPagesPtr == null) {
                    continue;
                }
                if (searchType == 3 && i == currentPage) {
                    pageIndexs.add(0, pageIndex);
                    textPagesPtrs.add(0, textPagesPtr);
                } else {
                    pageIndexs.add(pageIndex);
                    textPagesPtrs.add(textPagesPtr);
                }
            }
        }
        if (searchTextAsyncTask != null) {
            searchTextAsyncTask.cancel(true);
        }
        searchTextAsyncTask = new SearchTextAsyncTask(pdfiumCore, text, pageIndexs, textPagesPtrs, onSearchTextListener);
        searchTextAsyncTask.execute();
    }

    /**
     * Returns null if document is not loaded
     */
    public PdfDocument.Meta getDocumentMeta() {
        if (recycled || pdfFile == null) {
            return null;
        }
        return pdfFile.getMetaData();
    }

    /**
     * Will be empty until document is loaded
     */
    public List<PdfDocument.Bookmark> getTableOfContents() {
        if (recycled || pdfFile == null) {
            return Collections.emptyList();
        }
        return pdfFile.getBookmarks();
    }

    /**
     * Will be empty until document is loaded
     */
    public List<PdfDocument.Link> getLinks(int page) {
        if (recycled || pdfFile == null) {
            return Collections.emptyList();
        }
        return pdfFile.getPageLinks(page);
    }

    /**
     * Use an asset file as the pdf source
     */
    public Configurator fromAsset(String assetName) {
        return new Configurator(new AssetSource(assetName));
    }

    /**
     * Use a file as the pdf source
     */
    public Configurator fromFile(File file) {
        return new Configurator(new FileSource(file));
    }

    /**
     * Use URI as the pdf source, for use with content providers
     */
    public Configurator fromUri(Uri uri) {
        return new Configurator(new UriSource(uri));
    }

    /**
     * Use bytearray as the pdf source, documents is not saved
     */
    public Configurator fromBytes(byte[] bytes) {
        return new Configurator(new ByteArraySource(bytes));
    }

    /**
     * Use stream as the pdf source. Stream will be written to bytearray, because native code does not support Java Streams
     */
    public Configurator fromStream(InputStream stream) {
        return new Configurator(new InputStreamSource(stream));
    }

    /**
     * Use custom source as pdf source
     */
    public Configurator fromSource(DocumentSource docSource) {
        return new Configurator(docSource);
    }


    public BaseAnnotation removeLastDrawingPenAnnotation() {
       return  annotationManager.removeLastDrawingPenAnnotations();
    }

    public BaseAnnotation removeLastAnnotation(int page, boolean needNotify) {
        return annotationManager.removeLastAnnotation(page, needNotify);
    }

    public void addLastAnnotation(BaseAnnotation baseAnnotation, boolean needNotify) {
        annotationManager.addLastAnnotation(baseAnnotation, needNotify);
    }

    public void addAnnotations(@NonNull List<AnnotationBean> data, boolean needNotify) {
        List<BaseAnnotation> annotations = new ArrayList<>();
        for (AnnotationBean annotationBean : data) {
            try {
                if (annotationBean.type == AnnotationBean.TYPE_NORMAL) {
                    annotations.add(annotationBean.getAnnotation());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        annotationManager.addAnnotations(annotations, needNotify);
    }

    public void removeAnnotations(@NonNull List<AnnotationBean> data, boolean needNotify) {
        List<BaseAnnotation> annotations = new ArrayList<>();
        for (AnnotationBean annotationBean : data) {
            try {
                if (annotationBean.type == AnnotationBean.TYPE_NORMAL) {
                    annotations.add(annotationBean.getAnnotation());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        annotationManager.removeAnnotations(annotations, needNotify);
    }

    public List<AnnotationBean> getAllAnnotation() {
        List<BaseAnnotation> datas = annotationManager.getAllAnnotation();
        List<AnnotationBean> annotationBeans = new ArrayList<>();
        for (BaseAnnotation data : datas) {
            annotationBeans.add(new AnnotationBean(data, false));
        }
        return annotationBeans;
    }

    public List<AnnotationBean> getCurrentPageAllAnnotation(MarkAreaType markAreaType, int page) {
        List<BaseAnnotation> datas = annotationManager.getCurrentPageAllAnnotation(markAreaType,page);
        List<AnnotationBean> annotationBeans = new ArrayList<>();
        for (BaseAnnotation data : datas) {
            annotationBeans.add(new AnnotationBean(data, false));
        }
        return annotationBeans;
    }

    public List<AnnotationBean> getAllOptimizationAnnotation() {
        List<BaseAnnotation> datas = annotationManager.getAllAnnotation();
        List<AnnotationBean> annotationBeans = new ArrayList<>();
        for (BaseAnnotation data : datas) {
            annotationBeans.add(getOptimizationAnnotation(data));
        }
        return annotationBeans;
    }

    public AnnotationBean getOptimizationAnnotation(BaseAnnotation annotation) {
        AnnotationBean annotationBean;
        if (annotation instanceof MarkAnnotation) {
            annotationBean = new AnnotationBean(annotation, true);
        } else {
            annotationBean = new AnnotationBean(annotation, false);
        }
        return annotationBean;
    }

    /**
     * 从缓存删除一页的注释
     */
    public void removeAnnotation(@IntRange(from = 0) int page) {
        annotationManager.removeAnnotation(page);
    }

    /**
     * 从缓存删除所有注释
     */
    public void removeAnnotationAll() {
        annotationManager.removeAnnotationAll();
    }

    public void setAnnotationListener(AnnotationListener annotationListener) {
        annotationManager.setAnnotationListener(annotationListener);
    }

    public void setOnAreaTouchListener(OnAreaTouchListener onAreaTouchListener) {
        this.onAreaTouchListener = onAreaTouchListener;
    }

    public OnTextRemarkListener getOnTextRemarkListener() {
        return onTextRemarkListener;
    }

    public void setOnTextRemarkListener(OnTextRemarkListener onTextRemarkListener) {
        this.onTextRemarkListener = onTextRemarkListener;
    }

    public void setOnPdfViewClickListener(OnClickListener onClickListener) {
        this.onPdfViewClickListener = onClickListener;
    }

    public State getState() {
        return state;
    }

    public enum State {DEFAULT, LOADED, SHOWN, ERROR}

    /**
     * 功能
     */
    public enum Function {
        //预览pdf
        VIEWER,
        //画笔
        PEN,
        //橡皮擦
        ERASER,
        //标记
        MARK,
        //文字
        TEXT,
    }

    public class Configurator {

        private final DocumentSource documentSource;

        private int[] pageNumbers = null;

        private boolean enableSwipe = true;

        private boolean enableDoubletap = true;

        private OnDrawListener onDrawListener;

        private OnDrawListener onDrawAllListener;

        private OnLoadCompleteListener onLoadCompleteListener;

        private OnErrorListener onErrorListener;

        private OnPageChangeListener onPageChangeListener;

        private OnPageScrollListener onPageScrollListener;

        private OnRenderListener onRenderListener;

        private OnTapListener onTapListener;

        private OnLongPressListener onLongPressListener;

        private OnPageErrorListener onPageErrorListener;

        private LinkHandler linkHandler = new DefaultLinkHandler(PDFView.this);

        private int defaultPage = 0;

        private boolean swipeHorizontal = false;

        private boolean annotationRendering = false;

        private String password = null;

        private ScrollHandle scrollHandle = null;

        private boolean antialiasing = true;

        private int spacing = 0;

        private boolean autoSpacing = false;

        private boolean drawingPenOptimize = false;

        private FitPolicy pageFitPolicy = FitPolicy.WIDTH;

        private boolean fitEachPage = false;

        private boolean pageFling = false;

        private boolean pageSnap = false;

        private boolean nightMode = false;

        private boolean autoFillWhiteSpace = false;

        private boolean loadAfterCheckWhiteSpace = false;

        private boolean touchWithoutSpace = false;

        private boolean useMinWhiteSpaceZoom = false;

        private boolean initWhiteSpaceOptimization = true;

        private boolean isWhiteSpaceRenderBestQuality = false;

        private float whiteSpaceRenderThumbnailRatio = THUMBNAIL_RATIO;

        private int whiteSpaceRenderPageCountWhenOptimization = 12;

        private boolean showLoadingWhenWhiteSpaceRender = false;

        private boolean supportCustomRendering = false;

        private Bitmap cancelBitmap = null;

        private float cancelBitmapSize = 50;

        private float editTextRemarkFontSize = 12;

        private int editTextRemarkThemeColor;

        private int editTextNormalColor;

        private int editTextHintColor;

        private boolean readOnlyMode;

        private int annotationRenderingArea=1;

        private boolean singleZoom;

        private Configurator(DocumentSource documentSource) {
            this.documentSource = documentSource;
        }

        public Configurator pages(int... pageNumbers) {
            this.pageNumbers = pageNumbers;
            return this;
        }

        public Configurator enableSwipe(boolean enableSwipe) {
            this.enableSwipe = enableSwipe;
            return this;
        }

        public Configurator enableDoubletap(boolean enableDoubletap) {
            this.enableDoubletap = enableDoubletap;
            return this;
        }

        public Configurator enableAnnotationRendering(boolean annotationRendering) {
            this.annotationRendering = annotationRendering;
            return this;
        }

        public Configurator onDraw(OnDrawListener onDrawListener) {
            this.onDrawListener = onDrawListener;
            return this;
        }

        public Configurator onDrawAll(OnDrawListener onDrawAllListener) {
            this.onDrawAllListener = onDrawAllListener;
            return this;
        }

        public Configurator onLoad(OnLoadCompleteListener onLoadCompleteListener) {
            this.onLoadCompleteListener = onLoadCompleteListener;
            return this;
        }

        public Configurator onPageScroll(OnPageScrollListener onPageScrollListener) {
            this.onPageScrollListener = onPageScrollListener;
            return this;
        }

        public Configurator onError(OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
            return this;
        }

        public Configurator onPageError(OnPageErrorListener onPageErrorListener) {
            this.onPageErrorListener = onPageErrorListener;
            return this;
        }

        public Configurator onPageChange(OnPageChangeListener onPageChangeListener) {
            this.onPageChangeListener = onPageChangeListener;
            return this;
        }

        public Configurator onRender(OnRenderListener onRenderListener) {
            this.onRenderListener = onRenderListener;
            return this;
        }

        public Configurator onTap(OnTapListener onTapListener) {
            this.onTapListener = onTapListener;
            return this;
        }

        public Configurator onLongPress(OnLongPressListener onLongPressListener) {
            this.onLongPressListener = onLongPressListener;
            return this;
        }

        public Configurator linkHandler(LinkHandler linkHandler) {
            this.linkHandler = linkHandler;
            return this;
        }

        public Configurator defaultPage(int defaultPage) {
            this.defaultPage = defaultPage;
            return this;
        }

        public Configurator swipeHorizontal(boolean swipeHorizontal) {
            this.swipeHorizontal = swipeHorizontal;
            return this;
        }

        public Configurator password(String password) {
            this.password = password;
            return this;
        }

        public Configurator scrollHandle(ScrollHandle scrollHandle) {
            this.scrollHandle = scrollHandle;
            return this;
        }

        public Configurator enableAntialiasing(boolean antialiasing) {
            this.antialiasing = antialiasing;
            return this;
        }

        public Configurator spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Configurator autoSpacing(boolean autoSpacing) {
            this.autoSpacing = autoSpacing;
            return this;
        }

        public Configurator drawingPenOptimize(boolean drawingPenOptimize) {
            this.drawingPenOptimize = drawingPenOptimize;
            return this;
        }

        public Configurator pageFitPolicy(FitPolicy pageFitPolicy) {
            this.pageFitPolicy = pageFitPolicy;
            return this;
        }

        public Configurator fitEachPage(boolean fitEachPage) {
            this.fitEachPage = fitEachPage;
            return this;
        }

        public Configurator pageSnap(boolean pageSnap) {
            this.pageSnap = pageSnap;
            return this;
        }

        public Configurator pageFling(boolean pageFling) {
            this.pageFling = pageFling;
            return this;
        }

        public Configurator nightMode(boolean nightMode) {
            this.nightMode = nightMode;
            return this;
        }

        public Configurator setAutoFillWhiteSpace(boolean autoFillWhiteSpace) {
            this.autoFillWhiteSpace = autoFillWhiteSpace;
            return this;
        }

        public Configurator setSupportCustomRendering(boolean supportCustomRendering) {
            this.supportCustomRendering = supportCustomRendering;
            return this;
        }

        public Configurator setLoadAfterCheckWhiteSpace(boolean loadAfterCheckWhiteSpace) {
            this.loadAfterCheckWhiteSpace = loadAfterCheckWhiteSpace;
            return this;
        }

        public Configurator setTouchWithoutSpace(boolean touchWithoutSpace) {
            this.touchWithoutSpace = touchWithoutSpace;
            return this;
        }

        public Configurator setUseMinWhiteSpaceZoom(boolean useMinWhiteSpaceZoom) {
            this.useMinWhiteSpaceZoom = useMinWhiteSpaceZoom;
            return this;
        }

        public Configurator setInitWhiteSpaceOptimization(boolean initWhiteSpaceOptimization) {
            this.initWhiteSpaceOptimization = initWhiteSpaceOptimization;
            return this;
        }

        public Configurator setWhiteSpaceRenderBestQuality(boolean isWhiteSpaceRenderBestQuality) {
            this.isWhiteSpaceRenderBestQuality = isWhiteSpaceRenderBestQuality;
            return this;
        }

        public Configurator setWhiteSpaceRenderThumbnailRatio(float whiteSpaceRenderThumbnailRatio) {
            this.whiteSpaceRenderThumbnailRatio = whiteSpaceRenderThumbnailRatio;
            return this;
        }

        public Configurator setWhiteSpaceRenderPageCountWhenOptimization(int whiteSpaceRenderPageCountWhenOptimization) {
            this.whiteSpaceRenderPageCountWhenOptimization = whiteSpaceRenderPageCountWhenOptimization;
            return this;
        }

        public Configurator setShowLoadingWhenWhiteSpaceRender(boolean showLoadingWhenWhiteSpaceRender) {
            this.showLoadingWhenWhiteSpaceRender = showLoadingWhenWhiteSpaceRender;
            return this;
        }

        public Configurator setCancelBitmap(Bitmap cancelBitmap) {
            this.cancelBitmap = cancelBitmap;
            return this;
        }

        public Configurator setCancelBitmapSize(float cancelBitmapSize) {
            this.cancelBitmapSize = cancelBitmapSize;
            return this;
        }

        public Configurator setEditTextRemarkFontSize(int textRemarkFontSize) {
            this.editTextRemarkFontSize = textRemarkFontSize;
            return this;
        }

        public Configurator setEditTextRemarkThemeColor(int editTextRemarkThemeColor) {
            this.editTextRemarkThemeColor = editTextRemarkThemeColor;
            return this;
        }

        public Configurator setEditTextNormalColor(int editTextNormalColor) {
            this.editTextNormalColor = editTextNormalColor;
            return this;
        }

        public Configurator setEditTextHintColor(int editTextHintColor) {
            this.editTextHintColor = editTextHintColor;
            return this;
        }

        public DocumentSource getDocumentSource() {
            return documentSource;
        }


        public Configurator setReadOnlyMode(boolean readOnlyMode) {
            this.readOnlyMode = readOnlyMode;
            return this;
        }

        public Configurator setAnnotationRenderingArea(int annotationRenderingArea) {
            this.annotationRenderingArea = annotationRenderingArea;
            return this;
        }

        public Configurator disableLongPress() {
            PDFView.this.dragPinchManager.disableLongpress();
            return this;
        }

        public Configurator setSingleZoom(boolean singleZoom) {
            this.singleZoom = singleZoom;
            return this;
        }

        public void load() {
            if (!hasSize) {
                waitingDocumentConfigurator = this;
                return;
            }
            PDFView.this.recycle();
            PDFView.this.callbacks.setOnLoadComplete(onLoadCompleteListener);
            PDFView.this.callbacks.setOnError(onErrorListener);
            PDFView.this.callbacks.setOnDraw(onDrawListener);
            PDFView.this.callbacks.setOnDrawAll(onDrawAllListener);
            PDFView.this.callbacks.setOnPageChange(onPageChangeListener);
            PDFView.this.callbacks.setOnPageScroll(onPageScrollListener);
            PDFView.this.callbacks.setOnRender(onRenderListener);
            PDFView.this.callbacks.setOnTap(onTapListener);
            PDFView.this.callbacks.setOnLongPress(onLongPressListener);
            PDFView.this.callbacks.setOnPageError(onPageErrorListener);
            PDFView.this.callbacks.setLinkHandler(linkHandler);
            PDFView.this.setSwipeEnabled(enableSwipe);
            PDFView.this.setNightMode(nightMode);
            PDFView.this.enableDoubletap(enableDoubletap);
            PDFView.this.setDefaultPage(defaultPage);
            PDFView.this.setSwipeVertical(!swipeHorizontal);
            PDFView.this.enableAnnotationRendering(annotationRendering);
            PDFView.this.setScrollHandle(scrollHandle);
            PDFView.this.enableAntialiasing(antialiasing);
            PDFView.this.setSpacing(spacing);
            PDFView.this.setAutoSpacing(autoSpacing);
            PDFView.this.setDrawingPenOptimize(drawingPenOptimize);
            PDFView.this.setPageFitPolicy(pageFitPolicy);
            PDFView.this.setFitEachPage(fitEachPage);
            PDFView.this.setPageSnap(pageSnap);
            PDFView.this.setPageFling(pageFling);
            PDFView.this.setAutoFillWhiteSpace(autoFillWhiteSpace);
            PDFView.this.setSupportCustomRendering(supportCustomRendering);
            PDFView.this.setLoadAfterCheckWhiteSpace(loadAfterCheckWhiteSpace);
            PDFView.this.setTouchWithoutSpace(touchWithoutSpace);
            PDFView.this.setUseMinWhiteSpaceZoom(useMinWhiteSpaceZoom);
            PDFView.this.setInitWhiteSpaceOptimization(initWhiteSpaceOptimization);
            PDFView.this.setWhiteSpaceRenderBestQuality(isWhiteSpaceRenderBestQuality);
            PDFView.this.setWhiteSpaceRenderThumbnailRatio(whiteSpaceRenderThumbnailRatio);
            PDFView.this.setWhiteSpaceRenderPageCountWhenOptimization(whiteSpaceRenderPageCountWhenOptimization);
            PDFView.this.setShowLoadingWhenWhiteSpaceRender(showLoadingWhenWhiteSpaceRender);
            PDFView.this.setCancelBitmapSize(cancelBitmapSize);
            PDFView.this.setEditTextRemarkFontSize(editTextRemarkFontSize);
            PDFView.this.setEditTextRemarkThemeColor(editTextRemarkThemeColor);
            PDFView.this.setEditTextNormalColor(editTextNormalColor);
            PDFView.this.setEditTextHintColor(editTextHintColor);
            PDFView.this.setReadOnlyMode(readOnlyMode);
            PDFView.this.setAnnotationRenderingArea(annotationRenderingArea);
            PDFView.this.setSingleZoom(singleZoom);
            if (cancelBitmap == null && getCancelBitmap() == null) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.close);
                PDFView.this.setCancelBitmap(bitmap);
            }
            if (!autoFillWhiteSpace) {
                resetZoom();
            }
            if (pageNumbers != null) {
                PDFView.this.load(documentSource, password, pageNumbers);
            } else {
                PDFView.this.load(documentSource, password);
            }
        }
    }

    public interface OnPdfViewPenAreaClickListener {
        void onClick(MotionEvent event, int x, int y);
        void onLongClick(MotionEvent event, int x, int y);
    }

    public interface OnPdfViewTextPenAreaClickListener {
        void onClick(MotionEvent event, int x, int y);

        void onLongClick(MotionEvent event, int x, int y);
    }


    public interface OnPdfViewProcessClickListener {

        void processClick(MotionEvent event, boolean haveProcess);

    }
}


