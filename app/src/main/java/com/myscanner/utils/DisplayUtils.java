package com.myscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.myscanner.App;

/**
 * 屏幕布局、测量、单位转换工具
 */
public class DisplayUtils {

    /**
     * 获取屏幕宽高
     */
    public static Point getScreenSize() {
        WindowManager wm = (WindowManager) App.getApp().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        return new Point(width, height);
    }

    /**
     * 获取View的宽高
     */
    public static Point measureViewSize(View view) {
        int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(width, height);
        height = view.getMeasuredHeight();
        width = view.getMeasuredWidth();
        return new Point(width, height);
    }

    /**
     * 弹出软键盘
     */
    public static void setShowKeyboardParamShow(Activity activity, EditText editText) {
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.requestFocus();
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

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
