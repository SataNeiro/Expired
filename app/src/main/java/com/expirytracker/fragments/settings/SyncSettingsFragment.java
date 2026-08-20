package com.expirytracker.fragments.settings;
import android.content.Context; import android.content.Intent; import android.content.SharedPreferences;
import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.TextView; import androidx.annotation.NonNull; import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment; import com.expirytracker.LoginActivity; import com.expirytracker.MainActivity;
import com.expirytracker.R; import java.text.SimpleDateFormat; import java.util.Date; import java.util.Locale;
public class SyncSettingsFragment extends Fragment {
    private TextView tvLastSync;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sync_settings, container, false);
        tvLastSync = view.findViewById(R.id.tv_last_sync);
        SharedPreferences prefs = requireActivity().getSharedPreferences("sync", Context.MODE_PRIVATE);
        long lastSyncTime = prefs.getLong("last_sync_time", 0);
        if (lastSyncTime > 0) {
            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(lastSyncTime));
            tvLastSync.setText("Последняя синхронизация: " + date);
        } else tvLastSync.setText("Последняя синхронизация: неизвестно");
        view.findViewById(R.id.btn_sync_now).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).syncNow(() -> {
                    SharedPreferences.Editor editor = requireActivity().getSharedPreferences("sync", Context.MODE_PRIVATE).edit();
                    editor.putLong("last_sync_time", System.currentTimeMillis()); editor.apply();
                    String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                    tvLastSync.setText("Последняя синхронизация: " + date);
                });
            }
        });
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            requireActivity().getSharedPreferences("user", 0).edit().clear().apply();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent); requireActivity().finish();
        });
        view.findViewById(R.id.btn_clear_local_db).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Очистка локальной базы")
                        .setMessage("Вы уверены, что хотите удалить все товары из локальной базы?\n\nДанные на сервере НЕ будут затронуты.")
                        .setPositiveButton("Очистить", (dialog, which) -> {
                            ((MainActivity) getActivity()).clearLocalDatabase();
                            SharedPreferences.Editor editor = requireActivity().getSharedPreferences("sync", Context.MODE_PRIVATE).edit();
                            editor.putLong("last_sync_time", System.currentTimeMillis()); editor.apply();
                            String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                            tvLastSync.setText("Последняя синхронизация: " + date);
                        })
                        .setNegativeButton("Отмена", null).show();
            }
        });
        return view;
    }
}
