package com.olinkstar.util;

import android.util.Log;

public class LOG
{
    public static boolean bLogVEnabled = true;
    public static boolean bLogDEnabled = true;
    public static boolean bLogIEnabled = true;
    public static boolean bLogWEnabled = true;
    public static boolean bLogEEnabled = true;

    public static int V(String tag, String msg)
    {
        if (bLogVEnabled) {
            Log.v(tag, msg);
        }
        return 0;
    }

    public static int D(String tag, String msg)
    {
        if (bLogDEnabled) {
            Log.d(tag, msg);
        }
        return 0;
    }

    public static int I(String tag, String msg)
    {
        if (bLogIEnabled) {
            Log.i(tag, msg);
        }
        return 0;
    }

    public static int W(String tag, String msg)
    {
        if (bLogWEnabled) {
            Log.w(tag, msg);
        }
        return 0;
    }

    public static int E(String tag, String msg)
    {
        if (bLogEEnabled) {
            Log.e(tag, msg);
        }
        return 0;
    }

    public static int E(String tag, String msg, Throwable tr)
    {
        if (bLogEEnabled) {
            Log.e(tag, msg, tr);
        }
        return 0;
    }
}