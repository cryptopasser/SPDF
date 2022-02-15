package com.snakeway.spdf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.RenderingCustomHandler;
import com.snakeway.pdfviewer.listener.OnLoadCompleteListener;
import com.snakeway.pdfviewer.util.FitPolicy;
import com.snakeway.spdf.adapter.ImageThumbnailAdapter;
import com.snakeway.spdf.databinding.ActivityThumbailBinding;
import com.snakeway.spdf.utils.BitmapMemoryCacheHelper;
import com.snakeway.spdf.utils.ScreenTool;

import java.util.ArrayList;
import java.util.List;

public class ThumbnailActivity extends BaseActivity<ActivityThumbailBinding> implements OnLoadCompleteListener {
    public static String RESULT_PAGE_KEY = "page";

    private View.OnClickListener onClickListener;
    private PDFView.Configurator configurator;
    private BitmapMemoryCacheHelper bitmapMemoryCacheHelper = new BitmapMemoryCacheHelper();

    private String filePath;
    private String filePassword;


    public static void openThumbnailActivity(Activity activity, String filePath, String filePassword, int requestCode) {
        Intent intent = new Intent(activity, ThumbnailActivity.class);
        intent.putExtra(MainActivity.FILE_PATH_KEY, filePath);
        intent.putExtra(MainActivity.FILE_PASSWORD_KEY, filePassword);
        if (requestCode == -1) {
            activity.startActivity(intent);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        filePath = intent.getStringExtra(MainActivity.FILE_PATH_KEY);
        filePassword = intent.getStringExtra(MainActivity.FILE_PASSWORD_KEY);
        initAll();
    }

    @Override
    protected ActivityThumbailBinding getViewBinder() {
        return ActivityThumbailBinding.inflate(getLayoutInflater());
    }

    @Override
    public void initHandler() {
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void initUi() {
        onClickListener();
    }

    @Override
    public void initConfigUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int statusBarHeight = ScreenTool.getStatusBarHeight(ThumbnailActivity.this);
            ViewGroup.LayoutParams layoutParams = viewBinding.layoutAppbar.toolbar.getLayoutParams();
            if (layoutParams != null) {
                layoutParams.height = layoutParams.height + statusBarHeight;
            }
            RelativeLayout.MarginLayoutParams relativeLayoutToolbarLayoutParams = (RelativeLayout.MarginLayoutParams) viewBinding.layoutAppbar.relativeLayoutToolbar.getLayoutParams();
            if (relativeLayoutToolbarLayoutParams != null) {
                relativeLayoutToolbarLayoutParams.topMargin = statusBarHeight;
            }
        }
        viewBinding.layoutAppbar.textViewToolbarLeft.setText(getString(R.string.activity_thumbail_title));
        openPdf(filePath, filePassword);
    }

    @Override
    public void initHttp() {
    }

    @Override
    public void initOther() {

    }

    private void onClickListener() {
        onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.imageViewBack:
                        doBack();
                        break;
                    default:
                        break;
                }
            }
        };
        viewBinding.layoutAppbar.imageViewBack.setOnClickListener(onClickListener);
    }

    private void openPdf(@NonNull String assetFileName, @Nullable String password) {
        configurator = viewBinding.pdfView.fromAsset(assetFileName);
        configurator.password(password)
                .swipeHorizontal(true)
                .pageSnap(false)
                .onLoad(this)
                .pageFitPolicy(FitPolicy.HEIGHT)
                .enableAnnotationRendering(false)
                .linkHandler(null)
                .setSupportCustomRendering(true)
                .load();
    }

    private List<RenderingCustomHandler.RenderingCustomPageInfo> getAllRenderingCustomPageInfos() {
        int pageCount = viewBinding.pdfView.getPageCount();
        List<RenderingCustomHandler.RenderingCustomPageInfo> pages = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            SizeF size = viewBinding.pdfView.getPageSize(i);
            pages.add(new RenderingCustomHandler.RenderingCustomPageInfo(i, size.getWidth(), size.getHeight(), new RectF(0, 0, 1, 1)));
        }
        return pages;
    }

    public void choosePage(int page) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_PAGE_KEY, page);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void getRenderingImages(List<RenderingCustomHandler.RenderingCustomPageInfo> pages, RenderingCustomHandler.OnRenderingCustomListener onRenderingCustomListener) {
        if (pages == null) {
            return;
        }
        RenderingCustomHandler.RenderingCustomTask renderingCustomTask = new RenderingCustomHandler.RenderingCustomTask(pages, true, false, false, onRenderingCustomListener);
        renderingCustomTask.thumbnailRatio = 0.2f;
        viewBinding.pdfView.addRenderingCustomTask(renderingCustomTask);
    }

    @Override
    public void doBack() {
        super.doBack();
        finish();
    }

    @Override
    public void onDestroy() {
        if (viewBinding.pdfView != null) {
            viewBinding.pdfView.recycle();
        }
        super.onDestroy();
    }

    @Override
    public void loadComplete(int nbPages) {
        viewBinding.gridView.setAdapter(new ImageThumbnailAdapter(ThumbnailActivity.this, bitmapMemoryCacheHelper, getAllRenderingCustomPageInfos(), 0));
    }
}
