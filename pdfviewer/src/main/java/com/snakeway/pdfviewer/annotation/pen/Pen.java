package com.snakeway.pdfviewer.annotation.pen;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.snakeway.pdflibrary.util.SizeF;
import com.snakeway.pdfviewer.CoordinateUtils;
import com.snakeway.pdfviewer.PDFView;
import com.snakeway.pdfviewer.model.TextRemarkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author snakeway
 */
public interface Pen<T> extends Cloneable {

    void draw(T data, Canvas canvas, float scale, int basePenWidth, PDFView pdfView, int page);

    PenType getPenType();

    void init();

    interface WritePen extends Pen<List<SizeF>> {
    }

    interface MarkPen extends Pen<List<RectF>> {

        default List<RectF> mergeRect(List<RectF> rects, PDFView pdfView, int page) {
            RectF indexLine = null;
            List<RectF> lineRects = new ArrayList<>();
            if (rects == null || rects.size() == 0) {
                return lineRects;
            }
            for (int i = 0; i < rects.size(); i++) {
                RectF f = CoordinateUtils.toPdfPointCoordinateDesc(pdfView, page, rects.get(i));
                if (f.isEmpty()) {
                    continue;
                }
                if (indexLine == null) {
                    indexLine = new RectF(f.left, f.top, f.right, f.bottom);
                    lineRects.add(indexLine);
                    continue;
                }
                if (indexLine.bottom < f.top) {//判断矩形是否属于同一行,如果新的区域顶部超过旧的底部说明需要换行
                    indexLine = new RectF(f.left, f.top, f.right, f.bottom);
                    lineRects.add(indexLine);
                } else {
                    indexLine.left = Math.min(indexLine.left, f.left);
                    indexLine.top = Math.min(indexLine.top, f.top);
                    indexLine.right = Math.max(indexLine.right, f.right);
                    indexLine.bottom = Math.max(indexLine.bottom, f.bottom);
                }
            }
            return lineRects;
        }
    }

    interface TextPen extends Pen<TextRemarkInfo> {
    }

}
