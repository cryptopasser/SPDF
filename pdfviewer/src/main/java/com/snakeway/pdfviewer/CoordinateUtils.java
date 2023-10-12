package com.snakeway.pdfviewer;

import android.graphics.Point;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;

import com.snakeway.pdflibrary.util.Size;
import com.snakeway.pdflibrary.util.SizeF;

/**
 * @author snakeway
 */
public class CoordinateUtils {
    public static final int DPI_SIZE = 72;//SIZE = DisplayMetrics.DENSITY_MEDIUM;//160对应dpi的1倍倍率,即pdf原始尺寸

    /**
     * 计算当前view点所在的pdf页面对应坐标,数组宽度3,返回所在页,x和y坐标
     */
    @Nullable
    public static int[] getPdfCoordinate(PDFView pdfView, float viewX, float viewY) {
        float zoom = pdfView.getZoom();
        //view原点相对于display原点的x位置,基于display坐标
        float xOffset = pdfView.getCurrentXOffset() / zoom;
        //view原点相对于display原点的y位置,基于display坐标
        float yOffset = pdfView.getCurrentYOffset() / zoom;
        //pdf页码每页间距
        int spacingPx = pdfView.getSpacingPx();
        //在display坐标系中,以view原点为原点,点击的点的x的坐标
        float x = viewX / zoom;
        //在display坐标系中,以view原点为原点,点击的点的y的坐标
        float y = viewY / zoom;
        //转换xy,点击点位于display坐标系中的位置
        x = xOffset - x;
        y = yOffset - y;

        int findPage = 0;
        boolean finded = false;
        //顺序查找当前位置所属页面，每次查找如果没有中AOD x | y 平移一页
        while (findPage < pdfView.getPageCount()) {
            if (pdfView.isSwipeVertical()) {
                float pageHeight = getDisplayPageHeight(pdfView, findPage);
                if (-1 * y < pageHeight + spacingPx / 2) {
                    finded = true;
                    break;
                }
                y += pageHeight;
                y += spacingPx;
            } else {
                float pageWidth = getDisplayPageWidth(pdfView, findPage);
                if (-1 * x < pageWidth + spacingPx / 2) {
                    finded = true;
                    break;
                }
                x += pageWidth;
                x += spacingPx;
            }
            findPage++;
        }
        if (!finded) {
            // throw new RuntimeException("get pdf coordinate error");
            Log.e("getPdfCoordinate", "get pdf coordinate error");
            return null;
        }
        //减去当前页的自动边距
        if (pdfView.isAutoSpacingEnabled()) {
            if (pdfView.isSwipeVertical()) {
                y += pdfView.pdfFile.getPageSpacing(findPage, 1) / 2;
            } else {
                x += pdfView.pdfFile.getPageSpacing(findPage, 1) / 2;
            }
        }
        //如果当前不是最大页面,减去页面大小带来的差距
        if (pdfView.isSwipeVertical()) {
            if (pdfView.pdfFile.getMaxPageWidth(pdfView.getCurrentPage()) > pdfView.pdfFile.pageSizes.get(findPage).getWidth()) {
                x += (pdfView.pdfFile.getMaxPageWidth(pdfView.getCurrentPage()) - pdfView.pdfFile.pageSizes.get(findPage).getWidth()) / 2;
            }
        } else {
            if (pdfView.pdfFile.getMaxPageHeight(pdfView.getCurrentPage()) > pdfView.pdfFile.pageSizes.get(findPage).getHeight()) {
                y += (pdfView.pdfFile.getMaxPageHeight(pdfView.getCurrentPage()) - pdfView.pdfFile.pageSizes.get(findPage).getHeight()) / 2;
            }
        }
        float xZoom = pdfView.pdfFile.pageSizes.get(findPage).getWidth() / pdfView.pdfFile.originalPageSizes.get(findPage).getWidth();
        x /= xZoom * -1;
        float yZoom = pdfView.pdfFile.pageSizes.get(findPage).getHeight() / pdfView.pdfFile.originalPageSizes.get(findPage).getHeight();
        y /= yZoom * -1;
        return new int[]{findPage, Math.round(x), Math.round(y)};
    }

    /**
     * 把pdf屏幕坐标转换为pdf坐标
     */
    public static SizeF toPdfPointCoordinate(PDFView pdfView, int page, int x, int y) {
        y = pdfView.pdfFile.originalPageSizes.get(page).getHeight() - y;
        return new SizeF(x * DPI_SIZE / pdfView.pdfiumCore.mCurrentDpi, y * DPI_SIZE / pdfView.pdfiumCore.mCurrentDpi);
    }

    /**
     * 把pdf坐标转换为pdf屏幕坐标
     */
    public static Point toPdfPointCoordinateDesc(PDFView pdfView, int page, float x, float y) {
        x *= pdfView.pdfiumCore.mCurrentDpi;
        x /= DPI_SIZE;
        y *= pdfView.pdfiumCore.mCurrentDpi;
        y /= DPI_SIZE;
        y = pdfView.pdfFile.originalPageSizes.get(page).getHeight() - y;
        return new Point((int) x, (int) y);
    }

    /**
     * 把pdf坐标转换为pdf屏幕坐标
     */
    public static RectF toPdfPointCoordinateDesc(PDFView pdfView, int page, RectF rectF) {
        Point leftTop = toPdfPointCoordinateDesc(pdfView, page, rectF.left, rectF.top);
        Point rightBottom = toPdfPointCoordinateDesc(pdfView, page, rectF.right, rectF.bottom);
        RectF f = new RectF();
        f.left = leftTop.x;
        f.top = leftTop.y;
        f.right = rightBottom.x;
        f.bottom = rightBottom.y;
        return f;
    }

    /**
     * 获取当前页面在Display坐标系的宽度（display坐标）
     */
    public static float getDisplayPageWidth(PDFView pdfView, int page) {
        if (page < 0 || page > pdfView.getPageCount() - 1) {
            return 0F;
        }
        float width = 0F;
        width += pdfView.pdfFile.pageSizes.get(page).getWidth();
        if (pdfView.isAutoSpacingEnabled()) {
            width += pdfView.pdfFile.getPageSpacing(page, 1);
        }
        return width;
    }

    /**
     * 获取当前页面在Display坐标系的高度
     */
    public static float getDisplayPageHeight(PDFView pdfView, int page) {
        if (page < 0 || page > pdfView.getPageCount() - 1) {
            return 0F;
        }
        float height = 0F;
        height += pdfView.pdfFile.pageSizes.get(page).getHeight();
        if (pdfView.isAutoSpacingEnabled()) {
            height += pdfView.pdfFile.getPageSpacing(page, 1);
        }
        return height;
    }

    /**
     * 获取页码的尺寸（pdf坐标）
     */
    public static Size getPdfPageSize(PDFView pdfView, int page) {
        return pdfView.pdfFile.originalPageSizes.get(page);
    }

    /**
     * 获取页面pdf和display比例
     */
    public static float getPdfDisplayRation(PDFView pdfView, int page) {
        return pdfView.pdfFile.originalPageSizes.get(page).getWidth() / pdfView.pdfFile.pageSizes.get(page).getWidth();
    }

}
