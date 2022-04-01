package com.snakeway.pdfviewer.annotation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.snakeway.pdfviewer.annotation.base.BaseAnnotation;
import com.snakeway.pdfviewer.annotation.pen.PenBuilder;
import com.snakeway.pdfviewer.annotation.pen.PenType;

/**
 * @author snakeway
 */
public class AnnotationBean {
    public static final int TYPE_NORMAL=0;
    public static final int TYPE_IMAGE=1;

    @SerializedName("penType")
    public PenType penType;
    @SerializedName("data")
    public String data;
    @SerializedName("page")
    public int page;
    @SerializedName("id")
    public String id;
    @SerializedName("type")
    public int type;

    public AnnotationBean(PenType penType, String data, int page, String id, int type) {
        this.penType = penType;
        this.data = data;
        this.page = page;
        this.id = id;
        this.type=type;
    }

    public AnnotationBean(BaseAnnotation baseAnnotation, boolean needOptimization) {
        penType = baseAnnotation.pen.getPenType();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.toJsonTree(baseAnnotation).getAsJsonObject();
        String penString = jsonObject.get("pen").toString();
        jsonObject.remove("pen");
        jsonObject.addProperty("pen", penString);
        id = jsonObject.get("id").getAsString();
        page = jsonObject.get("page") != null ? jsonObject.get("page").getAsInt() : 0;
        if (needOptimization) {
            jsonObject.remove("data");
            data = jsonObject.toString();
        } else {
            data = jsonObject.toString();
        }
        type=TYPE_NORMAL;
    }

    public BaseAnnotation getAnnotation() throws Exception {
        if(type==TYPE_IMAGE){
            return null;
        }
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        String penString = jsonObject.get("pen").getAsString();
        jsonObject.remove("pen");
        BaseAnnotation annotation = null;
        switch (penType) {
            case COLORPEN:
            case BRUSHPEN:
                annotation = gson.fromJson(jsonObject, PenAnnotation.class);
                break;
            case HIGHLIGHTPEN:
            case DELETELINE:
            case UNDERLINE:
            case UNDERWAVELINE:
                MarkAnnotation markAnnotation = gson.fromJson(jsonObject, MarkAnnotation.class);
                markAnnotation.needInit = true;
                annotation = markAnnotation;
                break;
            case TEXTPEN:
                annotation = gson.fromJson(jsonObject, TextAnnotation.class);
                break;
            default:
                throw new RuntimeException("error pen type");
        }
        annotation.pen = PenBuilder.getPen(penType, penString, gson);
        return annotation;
    }
}
