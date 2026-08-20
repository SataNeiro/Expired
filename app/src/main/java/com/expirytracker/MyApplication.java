package com.expirytracker;
import android.app.Application; import android.util.Log;
import com.expirytracker.network.RetrofitClient;
public class MyApplication extends Application {
    @Override public void onCreate() { super.onCreate(); RetrofitClient.init(this);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> { Log.e("ExpiryTracker", "Uncaught exception", throwable); });
    }
}
