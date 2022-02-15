package com.snakeway.spdf;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

/**
 * @author snakeway
 * @description:
 * @date :2021/7/23 14:47
 */
public class CustomView extends View {

    public CustomView(Context context) {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float scale = 1;
        float scaledSizeInPixels = this.getResources().getDisplayMetrics().scaledDensity;
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        TextItem textItem = new TextItem(1, 100, "测试数据", 0, 0);
        try {
            textPaint.setTextSize((float) textItem.size * scale);
            StaticLayout layout = new StaticLayout(textItem.text, textPaint, (int) (textItem.width * scale * scaledSizeInPixels),
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
            canvas.save();
            canvas.translate(textItem.x * scale, textItem.y * scale);
            layout.draw(canvas);
            canvas.restore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class TextItem {
        private float size;
        private float width;
        private String text;
        private float x;
        private float y;

        public TextItem(float size, float width, String text, float x, float y) {
            this.size = size;
            this.width = width;
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public float getSize() {
            return size;
        }

        public void setSize(float size) {
            this.size = size;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }

}


