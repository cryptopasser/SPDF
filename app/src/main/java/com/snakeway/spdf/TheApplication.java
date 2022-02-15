package com.snakeway.spdf;

import android.Manifest;
import android.app.Application;
import android.util.Log;

import com.blankj.utilcode.util.Utils;
import com.snakeway.fileviewer.utils.FileUtil;
import com.tencent.smtt.sdk.QbSdk;

import java.io.File;
import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/4 14:06
 */
public class TheApplication extends Application {

    public static final String TAG = "TheApplication";

    public static final int REQUEST_X5_PERMISSIONS = 1;

    public static final String[] BASE_X5_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    private static TheApplication theApplication;

    private boolean needInitX5 = true;

    @Override
    public void onCreate() {
        super.onCreate();
        theApplication = this;
        init();
    }

    private void init() {
        Utils.init(this);
        copyAssets();
        initX5Web();
    }

    private void copyAssets() {
        String filePath = new File(getFilesDir(), "test").getAbsolutePath() + File.separator;
        try {
            FileUtil.copyAssetsDir(this, "test", filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TheApplication getTheApplication() {
        return theApplication;
    }

    public boolean isNeedInitX5() {
        return needInitX5;
    }

    public void setNeedInitX5(boolean needInitX5) {
        this.needInitX5 = needInitX5;
    }

    public void initX5Web() {
        if (!EasyPermissions.hasPermissions(this, BASE_X5_PERMISSIONS)) {
            return;
        }
//        HashMap hashMap = new HashMap();
//        hashMap.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
//        hashMap.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
//        QbSdk.initTbsSettings(hashMap);
        setNeedInitX5(false);
        QbSdk.canLoadX5FirstTimeThirdApp(getApplicationContext());
        QbSdk.initX5Environment(getApplicationContext(), new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                Log.d(TAG, "onCoreInitFinished");
            }

            @Override
            public void onViewInitFinished(boolean initResult) {
                Log.e(TAG, "onViewInitFinished:" + initResult);
            }
        });
    }


}
