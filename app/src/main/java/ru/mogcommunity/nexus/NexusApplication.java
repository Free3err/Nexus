package ru.mogcommunity.rbrproject;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class NexusApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
