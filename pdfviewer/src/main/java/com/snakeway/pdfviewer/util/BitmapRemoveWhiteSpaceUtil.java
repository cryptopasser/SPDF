package com.snakeway.pdfviewer.util;

import android.graphics.Bitmap;


public class BitmapRemoveWhiteSpaceUtil {
    /**
     * 去除多余白框缩放比
     */
    public static float removeUnUseWhiteSpaceScale(Bitmap originBitmap, boolean bestQuality, boolean isWidth, float leavePercent) {
        int width = originBitmap.getWidth();
        int height = originBitmap.getHeight();
        int[] margin = getRemoveWhiteSpaceMargin(originBitmap, bestQuality);
        if (margin == null) {
            return 1;
        }
        int top = margin[0];
        int right = margin[1];
        int bottom = margin[2];
        int left = margin[3];

        int leaveSpaceHorizontal = (int) (width * leavePercent);
        int leaveSpaceVertical = (int) (height * leavePercent);
        if (top - leaveSpaceVertical > 0) {
            top = top - leaveSpaceVertical;
            bottom = bottom - leaveSpaceVertical;
        }
        if (left - leaveSpaceHorizontal > 0) {
            left = left - leaveSpaceHorizontal;
            right = right - leaveSpaceHorizontal;
        }

        float newWidthMarginScale = (float) (width - left - right) / width;
        float newHeightMarginScale = (float) (height - top - bottom) / height;
        if (isWidth) {
            return newWidthMarginScale;
        }
        return newHeightMarginScale;
    }

    /**
     * 去除多余白框
     */
    public static Bitmap removeUnUseWhiteSpace(Bitmap originBitmap, boolean bestQuality) {
        int width = originBitmap.getWidth();
        int height = originBitmap.getHeight();
        int[] margin = getRemoveWhiteSpaceMargin(originBitmap, bestQuality);
        if (margin == null) {
            return originBitmap;
        }
        int top = margin[0];
        int right = margin[1];
        int bottom = margin[2];
        int left = margin[3];

        float newWidthMarginScale = (float) (left + right) / width;
        float newHeightMarginScale = (float) (top + bottom) / height;
        if (newWidthMarginScale > newHeightMarginScale) {
            float ratio = newHeightMarginScale / newWidthMarginScale;
            left = Math.round(left * ratio);
            right = left;
        } else {
            float ratio = newWidthMarginScale / newHeightMarginScale;
            top = Math.round(top * ratio);
            bottom = top;
        }
        int horizontalMargin = left + right;
        int verticalMargin = bottom + top;

        if (horizontalMargin > width || verticalMargin > height) {
            return originBitmap;
        }

        int cropWidth = width - horizontalMargin;
        int cropHeight = height - verticalMargin;

        int[] newPix = new int[cropWidth * cropHeight];

        int i = 0;
        for (int h = top; h < top + cropHeight; h++) {
            for (int w = left; w < left + cropWidth; w++) {
                newPix[i++] = originBitmap.getPixel(w, h);
            }
        }
        Bitmap newBitmap = Bitmap.createBitmap(newPix, cropWidth, cropHeight, bestQuality ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        return newBitmap;
    }

    public static int[] getRemoveWhiteSpaceMargin(Bitmap originBitmap, boolean bestQuality) {
        int width = originBitmap.getWidth();
        int height = originBitmap.getHeight();

//        int[] imgThePixels = new int[width * height];
//        originBitmap.getPixels(
//                imgThePixels,
//                0,
//                width,
//                0,
//                0,
//                width,
//                height);
//
//        Bitmap bitmap = getGrayImg(
//                width,
//                height,
//                imgThePixels);

        int top = 0;
        int left = 0;
        int right = 0;
        int bottom = 0;

        for (int h = 0; h < height; h++) {
            boolean holdBlackPix = false;
            for (int w = 0; w < width; w++) {
                if (originBitmap.getPixel(w, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }

            if (holdBlackPix) {
                break;
            }
            top++;
        }

        for (int w = 0; w < width; w++) {
            boolean holdBlackPix = false;
            for (int h = 0; h < height; h++) {
                if (originBitmap.getPixel(w, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            left++;
        }


        for (int w = width - 1; w >= 0; w--) {
            boolean holdBlackPix = false;
            for (int h = 0; h < height; h++) {
                if (originBitmap.getPixel(w, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            right++;
        }

        for (int h = height - 1; h >= 0; h--) {
            boolean holdBlackPix = false;
            for (int w = 0; w < width; w++) {
                if (originBitmap.getPixel(w, h) != -1) {
                    holdBlackPix = true;
                    break;
                }
            }
            if (holdBlackPix) {
                break;
            }
            bottom++;
        }

        if (top + bottom >= height || left + right >= width) {
            return null;
        }
        int minVertical = Math.min(top, bottom);
        int minHorizontal = Math.min(left, right);

        top = minVertical;
        right = minHorizontal;
        bottom = minVertical;
        left = minHorizontal;
        int[] res = new int[4];
        res[0] = top;
        res[1] = right;
        res[2] = bottom;
        res[3] = left;
        return res;
    }

    /**
     * 灰度化 bitmap
     */
    private static Bitmap getGrayImg(int imgTheWidth, int imgTheHeight, int[] imgThePixels) {
        int alpha = 0xFF << 24;
        for (int i = 0; i < imgTheHeight; i++) {
            for (int j = 0; j < imgTheWidth; j++) {
                int grey = imgThePixels[imgTheWidth * i + j];
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                imgThePixels[imgTheWidth * i + j] = grey;
            }
        }
        Bitmap result =
                Bitmap.createBitmap(imgTheWidth, imgTheHeight, Bitmap.Config.RGB_565);
        result.setPixels(imgThePixels, 0, imgTheWidth, 0, 0, imgTheWidth, imgTheHeight);
        return result;
    }
}