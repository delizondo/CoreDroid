package com.coredroid.lite;

import android.app.Application;

public class CoreApplication extends Application {

    private static AppState state;

    {
        LogIt.d(CoreApplication.class, "Creating CoreApplication");
    }

    @Override
    public void onCreate() {
        LogIt.d(this, "Creating application state");
        state = new AppState(new PreferencesDataStore(this));
    }

    public static AppState getState() {
        return state;
    }
}