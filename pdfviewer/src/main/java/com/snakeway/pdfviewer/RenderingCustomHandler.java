package com.snakeway.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.snakeway.pdfviewer.model.RenderedCustomInfo;
import com.snakeway.pdfviewer.util.BitmapRemoveWhiteSpaceUtil;

import java.util.ArrayList;
import java.util.List;

public class RenderingCustomHandler extends Handler {
    static final int MSG_RENDERING_CUSTOM_TASK = 1;
    private PDFView pdfView;
    private RectF renderBounds = new RectF();
    private Rect roundedRenderBounds = new Rect();
    private Matrix renderMatrix = new Matrix();
    private boolean running = false;
    private int requestCount = 0;

    RenderingCustomHandler(Looper looper, PDFView pdfView) {
        super(looper);
        this.pdfView = pdfView;
    }

    void addRenderingCustomTask(RenderingCustomTask renderingCustomTask) {
        if (renderingCustomTask == null) {
            return;
        }
        Message msg = obtainMessage(MSG_RENDERING_CUSTOM_TASK, renderingCustomTask);
        sendMessage(msg);
    }

    @Override
    public void handleMessage(Message message) {
        RenderingCustomTask task = (RenderingCustomTask) message.obj;
        final OnRenderingCustomListener onWhiteSpaceInfoListener = task.onRenderingCustomListener;
        try {
            requestCount++;
            if (!running) {
                return;
            }
            final List<RenderedCustomInfo> renderedCustomInfos = proceed(task, onWhiteSpaceInfoListener);
            pdfView.post(new Runnable() {
                @Override
                public void run() {
                    if (onWhiteSpaceInfoListener != null) {
                        onWhiteSpaceInfoListener.onSuccess(renderedCustomInfos);
                    }
                }
            });
        } catch (Exception e) {
            pdfView.post(new Runnable() {
                @Override
                public void run() {
                    if (onWhiteSpaceInfoListener != null) {
                        onWhiteSpaceInfoListener.onError(e.getMessage());
                    }
                }
            });
        } finally {
            requestCount--;
        }
    }

    private List<RenderedCustomInfo> proceed(RenderingCustomTask renderingCustomTask, OnRenderingCustomListener onRenderingCustomListener) throws Exception {
        List<RenderedCustomInfo> renderedCustomInfos = new ArrayList<>();
        List<RenderingCustomPageInfo> pageInfos = renderingCustomTask.pageInfos;
        PdfFile pdfFile = pdfView.pdfFile;
        if (pageInfos == null || pdfFile == null) {
            return renderedCustomInfos;
        }
        boolean bestQuality = renderingCustomTask.bestQuality;
        float thumbnailRatio = renderingCustomTask.thumbnailRatio;

        for (int i = 0; i < pageInfos.size(); i++) {
            if (!running) {
                return renderedCustomInfos;
            }
            RenderingCustomPageInfo pageInfo = pageInfos.get(i);
            pdfFile.openPage(pageInfo.page);

            int w = Math.round(pageInfo.width);
            int h = Math.round(pageInfo.height);
            if (renderingCustomTask.thumbnail) {
                w = Math.round(w * thumbnailRatio);
                h = Math.round(h * thumbnailRatio);
            }

            if (w == 0 || h == 0 || pdfFile.pageHasError(pageInfo.page)) {
                continue;
            }

            Bitmap render;
            try {
                render = Bitmap.createBitmap(w, h, bestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                continue;
            }
            calculateBounds(w, h, pageInfo.bounds);

            pdfFile.renderPageBitmap(render, pageInfo.page, roundedRenderBounds, true);
            float scale = 1;
            int width = render.getWidth();
            int height = render.getHeight();
            int horizontalMargin = 0;
            int verticalMargin = 0;
            if (renderingCustomTask.removeWhiteSpace) {
                try {
                    float theScale = BitmapRemoveWhiteSpaceUtil.removeUnUseWhiteSpaceScale(render, renderingCustomTask.bestQuality, true, 0.02f);
                    scale = 1 / theScale;
                    horizontalMargin = (int) ((width - width * theScale) / 2);
                    verticalMargin = (int) ((height - height * theScale) / 2);
                    width = (int) (width * theScale);
                    height = (int) (height * theScale);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            RenderedCustomInfo renderedCustomInfo = new RenderedCustomInfo(pageInfo.page, width, height, scale, horizontalMargin, verticalMargin, horizontalMargin, verticalMargin, render);
            pdfView.post(new Runnable() {
                @Override
                public void run() {
                    if (onRenderingCustomListener != null) {
                        onRenderingCustomListener.onSuccessOne(renderedCustomInfo);
                    }
                }
            });
            renderedCustomInfos.add(renderedCustomInfo);
        }
        return renderedCustomInfos;
    }

    private void calculateBounds(int width, int height, RectF pageSliceBounds) {
        renderMatrix.reset();
        renderMatrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height);
        renderMatrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height());

        renderBounds.set(0, 0, width, height);
        renderMatrix.mapRect(renderBounds);
        renderBounds.round(roundedRenderBounds);
    }

    void stop() {
        running = false;
    }

    void start() {
        running = true;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public static class RenderingCustomTask {
        public List<RenderingCustomPageInfo> pageInfos;
        public boolean thumbnail;
        public boolean bestQuality;
        public boolean removeWhiteSpace;
        public OnRenderingCustomListener onRenderingCustomListener;
        public float thumbnailRatio = 1;

        public RenderingCustomTask(List<RenderingCustomPageInfo> pageInfos, boolean thumbnail, boolean bestQuality, boolean removeWhiteSpace, OnRenderingCustomListener onRenderingCustomListener) {
            this.pageInfos = pageInfos;
            this.thumbnail = thumbnail;
            this.bestQuality = bestQuality;
            this.removeWhiteSpace = removeWhiteSpace;
            this.onRenderingCustomListener = onRenderingCustomListener;
        }

    }

    public static class RenderingCustomPageInfo {
        int page;
        float width;
        float height;
        RectF bounds;

        public RenderingCustomPageInfo(int page, float width, float height, RectF bounds) {
            this.page = page;
            this.width = width;
            this.height = height;
            this.bounds = bounds;
        }
    }


    public interface OnRenderingCustomListener {

        void onSuccessOne(RenderedCustomInfo renderedCustomInfo);

        void onSuccess(List<RenderedCustomInfo> renderedCustomInfos);

        void onError(String error);

    }

}
