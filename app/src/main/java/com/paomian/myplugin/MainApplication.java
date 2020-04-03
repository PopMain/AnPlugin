package com.paomian.myplugin;

import android.app.Application;


public class MainApplication extends Application {
    private static Application sApp;
    @Override
    public void onCreate() {
        sApp = this;
        super.onCreate();
    }

    public static Application getApplication() {
        return sApp;
    }
}
