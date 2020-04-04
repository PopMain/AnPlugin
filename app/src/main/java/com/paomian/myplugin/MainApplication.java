package com.paomian.myplugin;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class MainApplication extends Application {
    private static Application sApp;
    private static Object sActivityThread;
    @Override
    public void onCreate() {
        sApp = this;
        sActivityThread = getActivityThread(this);
        super.onCreate();
    }

    public static Application getApplication() {
        return sApp;
    }

    public static Object getActivityThread() {
        if (sActivityThread == null) {
            sActivityThread = getActivityThread(sApp);
        }
        return sActivityThread;
    }

    private static Object getActivityThread(Context context) {
        try {
            Class activityThread = Class.forName("android.app.ActivityThread");
            // ActivityThread.currentActivityThread()
            Method m = activityThread.getMethod("currentActivityThread", new Class[0]);
            m.setAccessible(true);
            Object thread = m.invoke(null, new Object[0]);
            if (thread != null) return thread;

            // context.@mLoadedApk.@mActivityThread
            Field mLoadedApk = context.getClass().getField("mLoadedApk");
            mLoadedApk.setAccessible(true);
            Object apk = mLoadedApk.get(context);
            Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
            mActivityThreadField.setAccessible(true);
            return mActivityThreadField.get(apk);
        } catch (Throwable ignore) {
            throw new RuntimeException("Failed to get mActivityThread from context: " + context);
        }
    }
}
