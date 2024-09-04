package com.snakeway.spdf;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/4 14:06
 */
public class TheApplication extends Application {

    public static final String TAG = "TheApplication";
    private static TheApplication theApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        theApplication = this;
        init();
    }

    private void init() {
        Utils.init(this);
    }


    public static TheApplication getTheApplication() {
        return theApplication;
    }

}
