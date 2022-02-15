package com.snakeway.spdf.models;


public class BaseBookMarkBean {
    public String title;
    public long pageIndex;
    public OnBookMarkListener onBookMarkListener;


    public interface OnBookMarkListener {
        void onItemClick(BaseBookMarkBean baseBookMarkBean);
    }
}

