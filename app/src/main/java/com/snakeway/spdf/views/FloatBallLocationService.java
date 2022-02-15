package com.snakeway.spdf.views;

import android.content.Context;
import android.content.SharedPreferences;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.snakeway.floatball.LocationService;

@ServiceAgent
public class FloatBallLocationService implements LocationService {
    private SharedPreferences sharedPreferences;

    @Override
    public void onLocationChanged(int x, int y) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("floatball_x", x);
        editor.putInt("floatball_y", y);
        editor.apply();
    }

    @Override
    public int[] onRestoreLocation() {
        int[] location = {sharedPreferences.getInt("floatball_x", -1),
                sharedPreferences.getInt("floatball_y", -1)};//如果坐标点传的是-1，就不会恢复

        return location;
    }

    @Override
    public void start(Context context) {
        sharedPreferences = context.getSharedPreferences("floatball_location", Context.MODE_PRIVATE);
    }
}
