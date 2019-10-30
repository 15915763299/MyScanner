package com.myscanner.crash;

import com.myscanner.App;

/**
 * 因需要测试某些特殊设备的摄像头，这些设备无法连接USB线，所以需要将崩溃信息展示出来
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler mInstance;

    private CrashHandler() {
    }

    /**
     * 初始化
     */
    public static void init() {
        if (CrashHandler.mInstance == null) {
            CrashHandler.mInstance = new CrashHandler();
            // 设置该CrashHandler为程序的默认处理
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler.mInstance);
        }
    }

    /**
     * 异常捕获
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 跳转到崩溃提示Activity
        ActCrashInfo.crashJump(App.getApp(), ex);
        // 关闭已奔溃的app进程
        System.exit(0);
    }

}
