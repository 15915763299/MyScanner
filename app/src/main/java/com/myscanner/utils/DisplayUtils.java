package com.myscanner.utils;

import android.content.Context;

/**
 * 屏幕布局、测量、单位转换工具
 */
public class DisplayUtils {

    //*****************************************************************
    // * 单位转换
    //*****************************************************************

    public static int dip2px(Context context, float dip) {
        return (int) (dip * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static int sp2px(Context context, float sp) {
        return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }

    public static int px2dip(Context context, float px) {
        return (int) (px / context.getResources().getDisplayMetrics().density + 0.5f);
    }

}
