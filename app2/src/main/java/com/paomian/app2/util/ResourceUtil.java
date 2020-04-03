package com.paomian.app2.util;

import android.content.Context;

import com.paomian.app2.R;

public class ResourceUtil {

    public static String getString(Context context) {
        return context.getResources().getString(R.string.my_string);
    }
}
