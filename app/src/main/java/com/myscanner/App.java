package com.myscanner;

import android.app.Application;
import android.content.Context;

import com.myscanner.crash.CrashHandler;

public class App extends Application {

    private static App app;

    public static App getApp() {
        return app;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        app = this;
        CrashHandler.init();
    }
}
