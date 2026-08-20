package com.expirytracker.fragments.settings;
import android.content.Context; import android.content.SharedPreferences; import android.os.Bundle;
import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.EditText;
import android.widget.Toast; import androidx.annotation.NonNull; import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment; import androidx.work.ExistingPeriodicWorkPolicy; import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager; import com.expirytracker.R; import com.expirytracker.sync.SyncWorker;
import java.util.concurrent.TimeUnit;
public class GeneralSettingsFragment extends Fragment {
    private EditText etServerUrl, etWarningDays, etCriticalDays, etUnit, etSyncInterval;
    private SwitchCompat switchOffline; private SharedPreferences prefs;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_settings, container, false);
        etServerUrl = view.findViewById(R.id.et_server_url); etWarningDays = view.findViewById(R.id.et_warning_days);
        etCriticalDays = view.findViewById(R.id.et_critical_days); etUnit = view.findViewById(R.id.et_unit);
        etSyncInterval = view.findViewById(R.id.et_sync_interval); switchOffline = view.findViewById(R.id.switch_offline);
        prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        loadSettings();
        view.findViewById(R.id.btn_save_general).setOnClickListener(v -> saveSettings());
        return view;
    }
    private void loadSettings() {
        etServerUrl.setText(prefs.getString("server_url", "http://192.168.0.191"));
        etWarningDays.setText(String.valueOf(prefs.getInt("warning_days", 7)));
        etCriticalDays.setText(String.valueOf(prefs.getInt("critical_days", 0)));
        etUnit.setText(prefs.getString("unit", "шт"));
        etSyncInterval.setText(String.valueOf(prefs.getInt("sync_interval_minutes", 5)));
        switchOffline.setChecked(prefs.getBoolean("offline_mode", false));
    }
    private void saveSettings() {
        if (!isAdded()) return;
        String serverUrl = etServerUrl.getText().toString().trim();
        String daysStr = etWarningDays.getText().toString().trim();
        String criticalStr = etCriticalDays.getText().toString().trim();
        String unit = etUnit.getText().toString().trim();
        String intervalStr = etSyncInterval.getText().toString().trim();
        if (serverUrl.isEmpty() || daysStr.isEmpty() || criticalStr.isEmpty() || unit.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show(); return;
        }
        int days = Integer.parseInt(daysStr); int critical = Integer.parseInt(criticalStr); int interval = Integer.parseInt(intervalStr);
        if (interval < 1) { Toast.makeText(getContext(), "Интервал должен быть ≥ 1", Toast.LENGTH_SHORT).show(); return; }
        boolean offline = switchOffline.isChecked();
        prefs.edit().putString("server_url", serverUrl).putInt("warning_days", days).putInt("critical_days", critical)
             .putString("unit", unit).putInt("sync_interval_minutes", interval).putBoolean("offline_mode", offline).apply();
        if (getContext() != null) {
            WorkManager workManager = WorkManager.getInstance(getContext());
            if (offline) workManager.cancelUniqueWork("sync_work");
            else {
                PeriodicWorkRequest syncWork = new PeriodicWorkRequest.Builder(SyncWorker.class, interval, TimeUnit.MINUTES).build();
                workManager.enqueueUniquePeriodicWork("sync_work", ExistingPeriodicWorkPolicy.REPLACE, syncWork);
            }
        }
        Toast.makeText(getContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show();
    }
}
