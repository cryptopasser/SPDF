package com.snakeway.spdf.models;

/**
 * @author snakeway
 * @description:
 * @date :2021/7/8 9:02
 */


public class ReadModeItem {
    public enum ReadModeType {
        SINGLE, MULTI, AUTO_FILL_WHITE_SPACE, THUMBNAIL, NIGHT
    }

    private ReadModeType type;

    private int icon;

    private String title;

    private boolean isCheck;

    private boolean isShow = true;


    public ReadModeItem(ReadModeType type, int icon, String title, boolean isCheck) {
        this.type = type;
        this.icon = icon;
        this.title = title;
        this.isCheck = isCheck;
    }

    public ReadModeType getType() {
        return type;
    }

    public void setType(ReadModeType type) {
        this.type = type;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }


    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }
}
