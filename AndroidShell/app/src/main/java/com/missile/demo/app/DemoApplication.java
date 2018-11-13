package com.missile.demo.app;

import android.app.Application;
import android.util.Log;


public class DemoApplication extends Application {

    private static final String TAG = "DemoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "====== apk onCreate ======" + this);
    }
}
