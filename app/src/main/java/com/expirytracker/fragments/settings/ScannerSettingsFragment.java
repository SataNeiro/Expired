package com.expirytracker.fragments.settings;
import android.content.Context; import android.content.SharedPreferences; import android.os.Bundle;
import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup; import android.widget.EditText;
import android.widget.Toast; import androidx.annotation.NonNull; import androidx.fragment.app.Fragment;
import com.expirytracker.R;
public class ScannerSettingsFragment extends Fragment {
    private EditText etBroadcastAction, etBroadcastExtraData, etBroadcastExtraType, etBroadcastIntentCategory;
    private SharedPreferences prefs;
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scanner_settings, container, false);
        etBroadcastAction = view.findViewById(R.id.et_broadcast_action);
        etBroadcastExtraData = view.findViewById(R.id.et_broadcast_extra_data);
        etBroadcastExtraType = view.findViewById(R.id.et_broadcast_extra_type);
        etBroadcastIntentCategory = view.findViewById(R.id.et_broadcast_intent_category);
        prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        loadSettings();
        view.findViewById(R.id.btn_save_scanner).setOnClickListener(v -> saveSettings());
        return view;
    }
    private void loadSettings() {
        etBroadcastAction.setText(prefs.getString("broadcast_action", "com.hht.scanwedge"));
        etBroadcastExtraData.setText(prefs.getString("broadcast_extra_data", "com.hht.datawedge.data_string"));
        etBroadcastExtraType.setText(prefs.getString("broadcast_extra_type", "com.hht.datawedge.label_type"));
        etBroadcastIntentCategory.setText(prefs.getString("broadcast_intent_category", "android.intent.category.DEFAULT"));
    }
    private void saveSettings() {
        if (!isAdded()) return;
        String action = etBroadcastAction.getText().toString().trim();
        String data = etBroadcastExtraData.getText().toString().trim();
        String type = etBroadcastExtraType.getText().toString().trim();
        String category = etBroadcastIntentCategory.getText().toString().trim();
        if (action.isEmpty() || data.isEmpty() || type.isEmpty() || category.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show(); return;
        }
        prefs.edit().putString("broadcast_action", action).putString("broadcast_extra_data", data)
             .putString("broadcast_extra_type", type).putString("broadcast_intent_category", category).apply();
        Toast.makeText(getContext(), "Настройки сканера сохранены", Toast.LENGTH_SHORT).show();
    }
}
