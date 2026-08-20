package com.expirytracker;
import android.content.Intent; import android.content.SharedPreferences; import android.os.Bundle; import android.os.Handler;
import android.os.Looper; import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy; import androidx.work.PeriodicWorkRequest; import androidx.work.WorkManager;
import com.expirytracker.sync.SyncWorker; import java.util.concurrent.TimeUnit;
public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("logged_in", false);
            SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
            int interval = settings.getInt("sync_interval_minutes", 5);
            boolean offline = settings.getBoolean("offline_mode", false);
            if (!offline) {
                PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(SyncWorker.class, interval, TimeUnit.MINUTES).build();
                WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync_work", ExistingPeriodicWorkPolicy.KEEP, syncWork);
            }
            Intent intent = isLoggedIn ? new Intent(this, MainActivity.class) : new Intent(this, LoginActivity.class);
            startActivity(intent); finish();
        }, 1000);
    }
}
