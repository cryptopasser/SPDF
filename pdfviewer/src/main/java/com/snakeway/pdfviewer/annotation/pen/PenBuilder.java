package com.snakeway.pdfviewer.annotation.pen;

import androidx.annotation.ColorInt;

import com.google.gson.Gson;

/**
 * @author snakeway
 */
public final class PenBuilder {

    public static final Pen getPen(PenType penType, String jsonData, Gson gson) {
        Pen pen = null;
        switch (penType) {
            case COLORPEN:
                pen = gson.fromJson(jsonData, ColorPen.class);
                break;
            case BRUSHPEN:
                pen = gson.fromJson(jsonData, BrushPen.class);
                break;
            case HIGHLIGHTPEN:
                pen = gson.fromJson(jsonData, HighLightPen.class);
                break;
            case DELETELINE:
                pen = gson.fromJson(jsonData, DeleteLinePen.class);
                break;
            case UNDERLINE:
                pen = gson.fromJson(jsonData, UnderLinePen.class);
                break;
            case UNDERWAVELINE:
                pen = gson.fromJson(jsonData, UnderWaveLinePen.class);
                break;
            case TEXTPEN:
                pen = gson.fromJson(jsonData, TextPen.class);
                break;
            default:
                throw new RuntimeException("pen type error");
        }
        pen.init();
        return pen;
    }

    public static ColorPenBuilder colorPenBuilder() {
        return new ColorPenBuilder();
    }

    public static BrushPenBuilder brushPenBuilder() {
        return new BrushPenBuilder();
    }

    public static SelectedPenBuilder selectedPenBuilder() {
        return new SelectedPenBuilder();
    }

    public static DeleteLinePenBuilder deleteLinePenBuilder() {
        return new DeleteLinePenBuilder();
    }

    public static UnderLinePenBuilder underLinePenBuilder() {
        return new UnderLinePenBuilder();
    }

    public static UnderWaveLinePenBuilder underWaveLinePenBuilder() {
        return new UnderWaveLinePenBuilder();
    }

    public static AreaPenBuilder areaPenBuilder() {
        return new AreaPenBuilder();
    }

    public static SearchAreaPenBuilder searchAreaPenBuilder() {
        return new SearchAreaPenBuilder();
    }

    public static TextPenBuilder textPenBuilder() {
        return new TextPenBuilder();
    }

    static abstract class Builder {
        abstract public Pen build();
    }

    public static final class ColorPenBuilder extends Builder {
        ColorPenBuilder() {
        }

        int color;
        float penWidthScale;

        public ColorPenBuilder setColor(@ColorInt int color) {
            this.color = color;
            return this;
        }

        public ColorPenBuilder setPenWidthScale(float penWidthScale) {
            this.penWidthScale = penWidthScale;
            return this;
        }

        @Override
        public Pen.WritePen build() {
            return new ColorPen(color, penWidthScale);
        }
    }

    public static final class BrushPenBuilder extends Builder {
        BrushPenBuilder() {
        }

        int color;
        float penWidthScale;

        public BrushPenBuilder setColor(@ColorInt int color) {
            this.color = color;
            return this;
        }

        public BrushPenBuilder setPenWidthScale(float penWidthScale) {
            this.penWidthScale = penWidthScale;
            return this;
        }

        @Override
        public Pen.WritePen build() {
            return new BrushPen(color, penWidthScale);
        }
    }


    public static final class SelectedPenBuilder extends Builder {
        SelectedPenBuilder() {
        }

        int color;

        public SelectedPenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public HighLightPen build() {
            return new HighLightPen(color);
        }
    }

    public static final class DeleteLinePenBuilder extends Builder {

        DeleteLinePenBuilder() {
        }

        int color;

        public DeleteLinePenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public DeleteLinePen build() {
            return new DeleteLinePen(color);
        }
    }

    public static final class UnderLinePenBuilder extends Builder {
        UnderLinePenBuilder() {
        }

        int color;

        public UnderLinePenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public UnderLinePen build() {
            return new UnderLinePen(color);
        }
    }


    public static final class UnderWaveLinePenBuilder extends Builder {
        UnderWaveLinePenBuilder() {
        }

        int color;

        public UnderWaveLinePenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public UnderWaveLinePen build() {
            return new UnderWaveLinePen(color);
        }
    }


    public static final class AreaPenBuilder extends Builder {
        AreaPenBuilder() {
        }

        int color;

        public AreaPenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public AreaPen build() {
            return new AreaPen(color);
        }
    }

    public static final class SearchAreaPenBuilder extends Builder {
        SearchAreaPenBuilder() {
        }

        int color;

        public SearchAreaPenBuilder setColor(int color) {
            this.color = color;
            return this;
        }

        @Override
        public SearchAreaPen build() {
            return new SearchAreaPen(color);
        }
    }


    public static final class TextPenBuilder extends Builder {
        int color;
        float fontSize;

        TextPenBuilder() {
        }


        public TextPenBuilder setColor(int color,float fontSize) {
            this.color = color;
            this.fontSize=fontSize;
            return this;
        }

        @Override
        public TextPen build() {
            return new TextPen(color,fontSize);
        }
    }
}
