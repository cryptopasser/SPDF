package com.snakeway.pdfviewer;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.snakeway.pdfviewer.model.WhiteSpaceInfo;
import com.snakeway.pdfviewer.util.BitmapRemoveWhiteSpaceUtil;

import java.util.ArrayList;
import java.util.List;

class WhiteSpaceInfoHandler extends Handler {
    static final int MSG_WHITE_SPACE_TASK = 1;
    private PDFView pdfView;
    private RectF renderBounds = new RectF();
    private Rect roundedRenderBounds = new Rect();
    private Matrix renderMatrix = new Matrix();
    private boolean running = false;
    private int requestCount = 0;

    WhiteSpaceInfoHandler(Looper looper, PDFView pdfView) {
        super(looper);
        this.pdfView = pdfView;
    }

    void addWhiteSpaceInfoTask(WhiteSpaceInfoTask whiteSpaceInfoTask) {
        if (whiteSpaceInfoTask == null) {
            return;
        }
        Message msg = obtainMessage(MSG_WHITE_SPACE_TASK, whiteSpaceInfoTask);
        sendMessage(msg);
    }

    @Override
    public void handleMessage(Message message) {
        WhiteSpaceInfoTask task = (WhiteSpaceInfoTask) message.obj;
        final OnWhiteSpaceInfoListener onWhiteSpaceInfoListener = task.onWhiteSpaceInfoListener;
        try {
            requestCount++;
            if (!running) {
                pdfView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onWhiteSpaceInfoListener != null) {
                            onWhiteSpaceInfoListener.onError("OnWhiteSpaceInfoListener is not running");
                        }
                    }
                });
                return;
            }
            final List<WhiteSpaceInfo> whiteSpaceInfos = proceed(task, onWhiteSpaceInfoListener);
            pdfView.post(new Runnable() {
                @Override
                public void run() {
                    if (onWhiteSpaceInfoListener != null) {
                        onWhiteSpaceInfoListener.onSuccess(whiteSpaceInfos);
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

    private List<WhiteSpaceInfo> proceed(WhiteSpaceInfoTask whiteSpaceInfoTask, OnWhiteSpaceInfoListener onWhiteSpaceInfoListener) throws Exception {
        List<WhiteSpaceInfo> whiteSpaceInfos = new ArrayList<>();
        List<WhiteSpacePageInfo> pageInfos = whiteSpaceInfoTask.pageInfos;
        PdfFile pdfFile = pdfView.pdfFile;
        if (pageInfos == null || pdfFile == null) {
            return whiteSpaceInfos;
        }
        boolean isWhiteSpaceRenderBestQuality = pdfView.isWhiteSpaceRenderBestQuality();
        float thumbnailRatio = pdfView.getWhiteSpaceRenderThumbnailRatio();

        for (int i = 0; i < pageInfos.size(); i++) {
            if (!running) {
                return whiteSpaceInfos;
            }
            WhiteSpacePageInfo pageInfo = pageInfos.get(i);
            pdfFile.openPage(pageInfo.page);

            int w = Math.round(pageInfo.width);
            int h = Math.round(pageInfo.height);
            if (whiteSpaceInfoTask.thumbnail) {
                w = Math.round(w * thumbnailRatio);
                h = Math.round(h * thumbnailRatio);
            }

            if (w == 0 || h == 0 || pdfFile.pageHasError(pageInfo.page)) {
                continue;
            }

            Bitmap render;
            try {
                render = Bitmap.createBitmap(w, h, isWhiteSpaceRenderBestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
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
            try {
                float theScale = BitmapRemoveWhiteSpaceUtil.removeUnUseWhiteSpaceScale(render, whiteSpaceInfoTask.bestQuality, true, 0.02f);
                scale = 1 / theScale;
                horizontalMargin = (int) ((width - width * theScale) / 2);
                verticalMargin = (int) ((height - height * theScale) / 2);
                width = (int) (width * theScale);
                height = (int) (height * theScale);
            } catch (Exception e) {
                e.printStackTrace();
            }
            WhiteSpaceInfo whiteSpaceInfo = new WhiteSpaceInfo(pageInfo.page, width, height, scale, horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
            pdfView.post(new Runnable() {
                @Override
                public void run() {
                    if (onWhiteSpaceInfoListener != null) {
                        onWhiteSpaceInfoListener.onSuccessOne(whiteSpaceInfo);
                    }
                }
            });
            whiteSpaceInfos.add(whiteSpaceInfo);
        }
        return whiteSpaceInfos;
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

    public static class WhiteSpaceInfoTask {
        List<WhiteSpacePageInfo> pageInfos;
        boolean thumbnail;
        boolean bestQuality;
        OnWhiteSpaceInfoListener onWhiteSpaceInfoListener;

        public WhiteSpaceInfoTask(List<WhiteSpacePageInfo> pageInfos, boolean thumbnail, boolean bestQuality, OnWhiteSpaceInfoListener onWhiteSpaceInfoListener) {
            this.pageInfos = pageInfos;
            this.thumbnail = thumbnail;
            this.bestQuality = bestQuality;
            this.onWhiteSpaceInfoListener = onWhiteSpaceInfoListener;
        }
    }

    public static class WhiteSpacePageInfo {
        int page;
        float width;
        float height;
        RectF bounds;

        public WhiteSpacePageInfo(int page, float width, float height, RectF bounds) {
            this.page = page;
            this.width = width;
            this.height = height;
            this.bounds = bounds;
        }
    }


    public interface OnWhiteSpaceInfoListener {

        void onSuccessOne(WhiteSpaceInfo whiteSpaceInfo);

        void onSuccess(List<WhiteSpaceInfo> whiteSpaceInfos);

        void onError(String error);

    }

}
