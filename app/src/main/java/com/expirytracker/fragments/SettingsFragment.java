package com.expirytracker.fragments;
import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull; import androidx.annotation.Nullable; import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; import androidx.recyclerview.widget.RecyclerView;
import com.expirytracker.R; import com.expirytracker.fragments.settings.GeneralSettingsFragment;
import com.expirytracker.fragments.settings.ScannerSettingsFragment;
import com.expirytracker.fragments.settings.SyncSettingsFragment;
import java.util.ArrayList; import java.util.List;
public class SettingsFragment extends Fragment {
    private RecyclerView rvList; private FrameLayout container; private SettingsListAdapter adapter;
    private boolean isDetailShown = false;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        rvList = view.findViewById(R.id.rv_settings_list); this.container = view.findViewById(R.id.fragment_container);
        List<SettingsItem> items = new ArrayList<>();
        items.add(new SettingsItem("Общие", R.drawable.ic_settings));
        items.add(new SettingsItem("Сканер", R.drawable.ic_barcode));
        items.add(new SettingsItem("Синхронизация", R.drawable.ic_sync));
        adapter = new SettingsListAdapter(items, item -> {
            Fragment fragment;
            switch (item.title) {
                case "Общие": fragment = new GeneralSettingsFragment(); break;
                case "Сканер": fragment = new ScannerSettingsFragment(); break;
                case "Синхронизация": fragment = new SyncSettingsFragment(); break;
                default: fragment = new GeneralSettingsFragment();
            }
            showDetailFragment(fragment);
        });
        rvList.setAdapter(adapter);
        if (savedInstanceState != null && savedInstanceState.getBoolean("detail_shown", false)) showList();
        return view;
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) { super.onSaveInstanceState(outState); outState.putBoolean("detail_shown", isDetailShown); }
    private void showDetailFragment(Fragment fragment) {
        isDetailShown = true; rvList.setVisibility(View.GONE); container.setVisibility(View.VISIBLE);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); transaction.addToBackStack(null); transaction.commit();
    }
    private void showList() { isDetailShown = false; getChildFragmentManager().popBackStackImmediate(); rvList.setVisibility(View.VISIBLE); container.setVisibility(View.GONE); }
    @Override public void onResume() {
        super.onResume();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (isDetailShown) showList(); else { setEnabled(false); requireActivity().onBackPressed(); } }
        });
    }
}
