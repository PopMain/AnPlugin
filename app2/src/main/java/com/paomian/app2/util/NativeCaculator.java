package com.paomian.app2.util;

public class NativeCaculator {

    static {
        System.loadLibrary("caculator");
    }
    public static native int add();
}
