package com.expirytracker;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkInfo;

import com.bumptech.glide.Glide;
import com.expirytracker.db.ExpiryDbHelper;
import com.expirytracker.fragments.AddFragment;
import com.expirytracker.fragments.BaseFragment;
import com.expirytracker.fragments.ProductsFragment;
import com.expirytracker.fragments.SettingsFragment;
import com.expirytracker.models.BaseProduct;
import com.expirytracker.models.FoodProduct;
import com.expirytracker.models.Product;
import com.expirytracker.network.ApiService;
import com.expirytracker.network.RetrofitClient;
import com.expirytracker.sync.SyncWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static WeakReference<MainActivity> instance = null;
    private static String lastBarcode = "";
    private BarcodeListener barcodeListener = null;
    private List<Product> products = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private List<BaseProduct> baseProducts = new ArrayList<>();
    private List<FoodProduct> foodProducts = new ArrayList<>();
    private ProductsFragment productsFragment;
    private BaseFragment baseFragment;
    private SettingsFragment settingsFragment;
    private BarcodeReceiver barcodeReceiver;
    private View statusIndicator;
    private ProgressBar syncProgressBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ExpiryDbHelper dbHelper;
    private boolean offlineMode;
    private AddFragment addFragment;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LiveData<WorkInfo> syncWorkInfo;

    public interface BarcodeListener {
        void onBarcode(String barcode);
    }

    public static MainActivity getInstance() {
        return instance != null ? instance.get() : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = new WeakReference<>(this);
        RetrofitClient.init(this);

        SharedPreferences settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE);
        offlineMode = settingsPrefs.getBoolean("offline_mode", false);

        statusIndicator = findViewById(R.id.status_indicator);
        syncProgressBar = findViewById(R.id.sync_progress_bar);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
        updateNetworkStatus(isNetworkAvailable());

        String action = settingsPrefs.getString("broadcast_action", "com.hht.scanwedge");
        IntentFilter filter = new IntentFilter(action);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        barcodeReceiver = new BarcodeReceiver(barcode -> setBarcode(barcode));
        registerReceiver(barcodeReceiver, filter);

        dbHelper = new ExpiryDbHelper(this);
        loadAllDataAsync();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment fragment = null;
            if (id == R.id.nav_home) {
                if (addFragment == null) addFragment = new AddFragment();
                fragment = addFragment;
            } else if (id == R.id.nav_active) {
                if (productsFragment == null) productsFragment = new ProductsFragment();
                productsFragment.setShowArchived(false);
                productsFragment.setShowExpiredOnly(false);
                fragment = productsFragment;
            } else if (id == R.id.nav_base) {
                if (productsFragment == null) productsFragment = new ProductsFragment();
                productsFragment.setShowArchived(false);
                productsFragment.setShowExpiredOnly(true);
                fragment = productsFragment;
            } else if (id == R.id.nav_settings) {
                if (settingsFragment == null) settingsFragment = new SettingsFragment();
                fragment = settingsFragment;
            }
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
            }
            return true;
        });
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        if (!offlineMode) syncNow();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        if (barcodeReceiver != null) unregisterReceiver(barcodeReceiver);
        if (networkCallback != null && connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            connectivityManager.unregisterNetworkCallback(networkCallback);
        executor.shutdown();
        if (syncWorkInfo != null) {
            syncWorkInfo.removeObservers(this);
        }
    }

    public void setBarcodeListener(BarcodeListener listener) {
        this.barcodeListener = listener;
        if (listener != null && lastBarcode != null && !lastBarcode.isEmpty()) {
            listener.onBarcode(lastBarcode);
        }
    }

    public void setBarcode(String barcode) {
        lastBarcode = barcode;
        if (barcodeListener != null) barcodeListener.onBarcode(barcode);
    }

    private void loadAllDataAsync() {
        executor.execute(() -> {
            List<Product> loadedProducts = dbHelper.getAllActive();
            List<Product> loadedAllProducts = dbHelper.getAllProducts();
            List<BaseProduct> loadedBase = dbHelper.getAllBase();
            List<FoodProduct> loadedFood = dbHelper.getAllFood();
            mainHandler.post(() -> {
                products.clear();
                products.addAll(loadedProducts);
                allProducts.clear();
                allProducts.addAll(loadedAllProducts);
                baseProducts.clear();
                baseProducts.addAll(loadedBase);
                foodProducts.clear();
                foodProducts.addAll(loadedFood);
                refreshProductsList();
                refreshBaseList();
            });
        });
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Product> getAllProducts() {
        return allProducts;
    }

    public List<BaseProduct> getBaseProducts() {
        return baseProducts;
    }

    public List<FoodProduct> getFoodProducts() {
        return foodProducts;
    }

    public void refreshProductsList() {
        if (productsFragment != null) productsFragment.refreshList();
    }

    public void refreshBaseList() {
        if (baseFragment != null) baseFragment.refreshBase();
    }

    public void loadProductsFromDB() {
        executor.execute(() -> {
            List<Product> loaded = dbHelper.getAllActive();
            List<Product> loadedAll = dbHelper.getAllProducts();
            mainHandler.post(() -> {
                products.clear();
                products.addAll(loaded);
                allProducts.clear();
                allProducts.addAll(loadedAll);
                refreshProductsList();
            });
        });
    }

    public void loadBaseFromDB() {
        executor.execute(() -> {
            List<BaseProduct> loaded = dbHelper.getAllBase();
            mainHandler.post(() -> {
                baseProducts.clear();
                baseProducts.addAll(loaded);
                refreshBaseList();
            });
        });
    }

    public void loadFoodFromDB() {
        executor.execute(() -> {
            List<FoodProduct> loaded = dbHelper.getAllFood();
            mainHandler.post(() -> {
                foodProducts.clear();
                foodProducts.addAll(loaded);
            });
        });
    }

    public void insertProductLocally(Product p) {
        dbHelper.insertOrUpdate(p);
        loadProductsFromDB();
    }

    public void insertBaseProduct(BaseProduct bp) {
        dbHelper.insertOrUpdateBase(bp);
        loadBaseFromDB();
    }

    public void insertFoodProduct(FoodProduct fp) {
        dbHelper.insertOrUpdateFood(fp);
        loadFoodFromDB();
    }

    public void markProductSynced(String id) {
        dbHelper.markSynced(id);
        loadProductsFromDB();
    }

    public void syncNow() {
        syncNow(null);
    }

    public void syncNow(Runnable onComplete) {
        if (offlineMode) {
            if (onComplete != null) onComplete.run();
            return;
        }
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();

        if (syncWorkInfo != null) {
            syncWorkInfo.removeObservers(this);
        }

        syncProgressBar.setProgress(0);
        syncProgressBar.setVisibility(View.VISIBLE);

        syncWorkInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(syncWork.getId());
        syncWorkInfo.observe(this, workInfo -> {
            if (workInfo != null) {
                if (workInfo.getState().isFinished()) {
                    syncProgressBar.setVisibility(View.GONE);
                    if (onComplete != null) onComplete.run();
                    syncWorkInfo.removeObservers(this);
                    loadProductsFromDB();
                    loadBaseFromDB();
                    loadFoodFromDB();

                    // ---- Очистка кэша Glide ----
                    new Thread(() -> {
                        Glide.get(getApplicationContext()).clearDiskCache();
                    }).start();
                    Glide.get(getApplicationContext()).clearMemory();
                    // --------------------------

                } else if (workInfo.getProgress() != null) {
                    int progress = workInfo.getProgress().getInt("progress", 0);
                    syncProgressBar.setProgress(progress);
                }
            }
        });

        WorkManager.getInstance(this).enqueue(syncWork);
    }

    public void deleteProductFromDB(String id) {
        dbHelper.deleteProduct(id);
        loadProductsFromDB();
        if (!offlineMode) {
            JsonObject body = new JsonObject();
            body.addProperty("action", "delete_product");
            body.addProperty("id", id);
            try {
                ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
                api.deleteProduct(body).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {}

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteBaseProduct(String baseId) {
        List<BaseProduct> newBase = new ArrayList<>();
        for (BaseProduct bp : baseProducts) {
            if (!bp.baseId.equals(baseId)) newBase.add(bp);
        }
        baseProducts = newBase;
        dbHelper.clearBase();
        for (BaseProduct bp : baseProducts) dbHelper.insertOrUpdateBase(bp);
        refreshBaseList();
        if (!offlineMode) syncNow();
    }

    public void clearLocalDatabase() {
        dbHelper.clearAll();
        loadProductsFromDB();
        loadBaseFromDB();
        loadFoodFromDB();
    }

    public void fillFormFromBase(BaseProduct bp) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        if (addFragment != null) addFragment.fillFromBase(bp);
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            android.net.NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    private void updateNetworkStatus(boolean connected) {
        if (statusIndicator != null) {
            statusIndicator.setBackgroundColor(ContextCompat.getColor(this, connected ? R.color.server_connected : R.color.server_disconnected));
        }
    }

    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    runOnUiThread(() -> updateNetworkStatus(true));
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    runOnUiThread(() -> updateNetworkStatus(false));
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                    super.onCapabilitiesChanged(network, caps);
                    runOnUiThread(() -> updateNetworkStatus(true));
                }
            };
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }
}