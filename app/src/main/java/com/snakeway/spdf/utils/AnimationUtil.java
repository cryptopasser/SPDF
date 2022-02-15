package com.snakeway.spdf.utils;

import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

public class AnimationUtil {
    /**
     * 从控件所在位置移动到控件的底部
     */
    public static TranslateAnimation moveToViewBottom(Animation.AnimationListener animationListener) {
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);
        translateAnimation.setDuration(500);
        translateAnimation.setInterpolator(new DecelerateInterpolator());
        if (animationListener != null) {
            translateAnimation.setAnimationListener(animationListener);
        }
        return translateAnimation;
    }

    /**
     * 从控件的底部移动到控件所在位置
     */
    public static TranslateAnimation moveToViewLocation(Animation.AnimationListener animationListener) {
        TranslateAnimation translateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        translateAnimation.setDuration(500);
        translateAnimation.setInterpolator(new DecelerateInterpolator());
        if (animationListener != null) {
            translateAnimation.setAnimationListener(animationListener);
        }
        return translateAnimation;
    }
}