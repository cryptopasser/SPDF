package com.snakeway.spdf.models;


public class BaseBookMarkBean {
    public String title;
    public long pageIndex;
    public boolean isRemark;

    public OnBookMarkListener onBookMarkListener;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(long pageIndex) {
        this.pageIndex = pageIndex;
    }

    public boolean isRemark() {
        return isRemark;
    }

    public void setRemark(boolean remark) {
        isRemark = remark;
    }

    public OnBookMarkListener getOnBookMarkListener() {
        return onBookMarkListener;
    }

    public void setOnBookMarkListener(OnBookMarkListener onBookMarkListener) {
        this.onBookMarkListener = onBookMarkListener;
    }

    public interface OnBookMarkListener {
        void onItemClick(BaseBookMarkBean baseBookMarkBean);
    }
}

