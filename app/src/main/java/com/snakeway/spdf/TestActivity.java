package com.snakeway.spdf;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.snakeway.floatball.FloatBallManager;
import com.snakeway.floatball.menu.FloatMenuCfg;
import com.snakeway.floatball.menu.MenuItem;
import com.snakeway.floatball.permission.FloatPermissionManager;
import com.snakeway.floatball.utils.BackGroudSeletor;
import com.snakeway.floatball.utils.DensityUtil;
import com.snakeway.floatball.views.FloatBallCfg;


public class TestActivity extends Activity {
    private FloatBallManager mFloatBallManager;
    private FloatPermissionManager mFloatPermissionManager;
    private ActivityLifeCycleListener mActivityLifeCycleListener = new ActivityLifeCycleListener();
    private int resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        init(true);
        if (mFloatBallManager.getMenuItemSize() == 0) {
            mFloatBallManager.setOnFloatBallClickListener(new FloatBallManager.OnFloatBallClickListener() {
                @Override
                public void onFloatBallClick() {

                }
            });
        }
        getApplication().registerActivityLifecycleCallbacks(mActivityLifeCycleListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFloatBallManager.show();
        mFloatBallManager.onFloatBallClick();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFloatBallManager.hide();
    }

    private void init(boolean showMenu) {
        int ballSize = DensityUtil.dip2px(this, 45);
        Drawable ballIcon = BackGroudSeletor.getdrawble("icon_floatball", this);
        FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.LEFT_CENTER, false);
        ballCfg.setHideHalfLater(false);
        if (showMenu) {
            int menuSize = DensityUtil.dip2px(this, 180);
            int menuItemSize = DensityUtil.dip2px(this, 40);
            FloatMenuCfg menuCfg = new FloatMenuCfg(menuSize, menuItemSize);
            mFloatBallManager = new FloatBallManager(getApplicationContext(), ballCfg, menuCfg);
            addFloatMenuItem();
        } else {
            mFloatBallManager = new FloatBallManager(getApplicationContext(), ballCfg);
        }
        setFloatPermission();
    }

    private void setFloatPermission() {
        mFloatPermissionManager = new FloatPermissionManager();
        mFloatBallManager.setPermission(new FloatBallManager.IFloatBallPermission() {
            @Override
            public boolean onRequestFloatBallPermission() {
                requestFloatBallPermission(TestActivity.this);
                return true;
            }

            @Override
            public boolean hasFloatBallPermission(Context context) {
                return mFloatPermissionManager.checkPermission(context);
            }

            @Override
            public void requestFloatBallPermission(Activity activity) {
                mFloatPermissionManager.applyPermission(activity);
            }

        });
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

    private void addFloatMenuItem() {
        MenuItem menuItem1 = new MenuItem(BackGroudSeletor.getdrawble("icon_menu_1", this)) {
            @Override
            public void action() {
                mFloatBallManager.closeMenu();
            }
        };
        MenuItem menuItem2 = new MenuItem(BackGroudSeletor.getdrawble("icon_menu_2", this)) {
            @Override
            public void action() {
            }
        };
        MenuItem menuItem3 = new MenuItem(BackGroudSeletor.getdrawble("icon_menu_3", this)) {
            @Override
            public void action() {
                mFloatBallManager.closeMenu();
            }
        };
        MenuItem menuItemEmpty = new MenuItem(null) {
            @Override
            public void action() {
            }
        };
        mFloatBallManager.addMenuItem(menuItemEmpty).addMenuItem(menuItem1)
                .addMenuItem(menuItem2)
                .addMenuItem(menuItem3)
                .addMenuItem(menuItemEmpty)
                .buildMenu();
    }

    private void setFloatBallVisible(boolean visible) {
        if (visible) {
            mFloatBallManager.show();
        } else {
            mFloatBallManager.hide();
        }
    }

    private boolean isApplicationInForeground() {
        return resumed > 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplication().unregisterActivityLifecycleCallbacks(mActivityLifeCycleListener);
    }
}
