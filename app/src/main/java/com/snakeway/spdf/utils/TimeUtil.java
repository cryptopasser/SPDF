package com.snakeway.spdf.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author snakeway
 * @description:
 * @date :2021/3/10 9:14
 */
public class TimeUtil {

    private static long oldTime = -1;
    private static int count = 0;

    public static synchronized long getOnlyTime() {
        long time = System.currentTimeMillis();
        if (time == oldTime) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            time = System.currentTimeMillis();
        }
        oldTime = time;
        return time;
    }

    public static synchronized long getOnlyTimeWithoutSleep() {
        int ratio = 100000;
        if (count < ratio - 1) {
            count = count + 1;
        } else {
            count = 0;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long time = System.currentTimeMillis();
        time = time * ratio + count;
        return time;
    }

    public static String getUtcTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format(new Date());
    }


}
