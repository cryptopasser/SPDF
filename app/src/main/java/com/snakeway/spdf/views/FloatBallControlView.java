package com.snakeway.spdf.views;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.buyi.huxq17.serviceagency.exception.AgencyException;
import com.snakeway.floatball.FloatBallManager;
import com.snakeway.floatball.menu.FloatMenuCfg;
import com.snakeway.floatball.menu.MenuItem;
import com.snakeway.floatball.permission.FloatPermissionManager;
import com.snakeway.floatball.utils.BackGroudSeletor;
import com.snakeway.floatball.utils.DensityUtil;
import com.snakeway.floatball.views.FloatBallCfg;
import com.snakeway.spdf.BaseActivity;

import java.util.List;

public class FloatBallControlView extends FrameLayout {
    private Context context;
    private Activity activity;
    private FloatBallManager floatBallManager;
    private FloatPermissionManager floatPermissionManager;
    private ActivityLifeCycleListener activityLifeCycleListener = new ActivityLifeCycleListener();
    private int resumed;

    public FloatBallControlView(Context context) {
        this(context, null, 0);
    }

    public FloatBallControlView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatBallControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        activity = BaseActivity.getActivityByContext(context);
    }

    public void init(List<MenuItem> menuItems) {
        try {
            ServiceAgency.getService(FloatBallLocationService.class).start(context);
        } catch (AgencyException e) {
            e.printStackTrace();
        }
        activity.getApplication().registerActivityLifecycleCallbacks(activityLifeCycleListener);
        int ballSize = DensityUtil.dip2px(context, 45);
        Drawable ballIcon = BackGroudSeletor.getdrawble("icon_floatball", context);
        FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.LEFT_CENTER, false);
        ballCfg.setHideHalfLater(false);
        if (menuItems.size() > 0) {
            int menuSize = DensityUtil.dip2px(context, 180);
            int menuItemSize = DensityUtil.dip2px(context, 40);
            FloatMenuCfg menuCfg = new FloatMenuCfg(menuSize, menuItemSize);
            floatBallManager = new FloatBallManager(context.getApplicationContext(), ballCfg, menuCfg);
            addMenuItems(menuItems);
        } else {
            floatBallManager = new FloatBallManager(context.getApplicationContext(), ballCfg);
        }
        setFloatPermission();
    }

    private void setFloatPermission() {
        floatPermissionManager = new FloatPermissionManager();
        floatBallManager.setPermission(new FloatBallManager.IFloatBallPermission() {
            @Override
            public boolean onRequestFloatBallPermission() {
                requestFloatBallPermission(activity);
                return true;
            }

            @Override
            public boolean hasFloatBallPermission(Context context) {
                return floatPermissionManager.checkPermission(context);
            }

            @Override
            public void requestFloatBallPermission(Activity activity) {
                floatPermissionManager.applyPermission(activity);
            }

        });
    }

    private void addMenuItems(List<MenuItem> menuItems) {
        for (int i = 0; i < menuItems.size(); i++) {
            floatBallManager.addMenuItem(menuItems.get(i));
        }
        floatBallManager.buildMenu();
    }

    public class ActivityLifeCycleListener implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            ++resumed;
            setFloatBallVisible(true);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            --resumed;
            if (!isApplicationInForeground()) {
                setFloatBallVisible(false);
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    private void setFloatBallVisible(boolean visible) {
        if (visible) {
            floatBallManager.show();
        } else {
            floatBallManager.hide();
        }
    }

    public FloatBallManager getFloatBallManager() {
        return floatBallManager;
    }


    public boolean isApplicationInForeground() {
        return resumed > 0;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        BaseActivity.getActivityByContext(this.context).getApplication().unregisterActivityLifecycleCallbacks(activityLifeCycleListener);
    }

}
