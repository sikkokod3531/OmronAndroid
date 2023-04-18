package com.omronhealthcare.OmronConnectivitySample;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    final String TAG = "OmronApp";

    public static Context AppContext;

    private static App mInstance = null;

    public App() {
        mInstance = this;
    }

    public static App getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        AppContext = this;

    }
}
