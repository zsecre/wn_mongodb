package com.watchable;
import android.app.Application;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import androidx.appcompat.app.AppCompatDelegate;

public class WatchableApp extends Application {
    private static WatchableApp inst;
    public void onCreate() { 
        super.onCreate(); 
        inst = this; 
        
        if (AppPreferences.get(this).isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        NetworkOverlayManager.init(this);
        RemoteUpdateManager.init(this);
    }
    public static WatchableApp getInstance() { return inst; }
}
