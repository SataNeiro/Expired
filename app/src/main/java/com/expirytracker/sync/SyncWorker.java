package com.expirytracker.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.expirytracker.db.ExpiryDbHelper;
import com.expirytracker.models.BaseProduct;
import com.expirytracker.models.FoodProduct;
import com.expirytracker.models.Product;
import com.expirytracker.network.ApiService;
import com.expirytracker.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    private static final int BATCH_SIZE = 1000;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("offline_mode", false)) {
            Log.d(TAG, "Offline mode – синхронизация пропущена");
            return Result.success();
        }

        ExpiryDbHelper db = new ExpiryDbHelper(context);
        ApiService api = RetrofitClient.getClient(context).create(ApiService.class);
        Gson gson = new Gson();

        List<Product> activeProducts = db.getAllActive();
        List<BaseProduct> baseProducts = db.getAllBase();
        List<FoodProduct> foodProducts = db.getAllFood();

        int total = activeProducts.size() + baseProducts.size() + foodProducts.size();
        if (total == 0) {
            Log.d(TAG, "Нет данных для синхронизации");
            return Result.success();
        }

        List<List<Product>> activeBatches = splitList(activeProducts, BATCH_SIZE);
        List<List<BaseProduct>> baseBatches = splitList(baseProducts, BATCH_SIZE);
        List<List<FoodProduct>> foodBatches = splitList(foodProducts, BATCH_SIZE);

        int sent = 0;
        int totalBatches = activeBatches.size() + baseBatches.size() + foodBatches.size();
        int processedBatches = 0;

        for (List<Product> batch : activeBatches) {
            if (!sendBatch(batch, api, gson)) {
                return Result.failure();
            }
            sent += batch.size();
            processedBatches++;
            updateProgress(total, sent, processedBatches, totalBatches);
        }

        for (List<BaseProduct> batch : baseBatches) {
            if (!sendBaseBatch(batch, api, gson)) {
                return Result.failure();
            }
            sent += batch.size();
            processedBatches++;
            updateProgress(total, sent, processedBatches, totalBatches);
        }

        for (List<FoodProduct> batch : foodBatches) {
            if (!sendFoodBatch(batch, api, gson)) {
                return Result.failure();
            }
            sent += batch.size();
            processedBatches++;
            updateProgress(total, sent, processedBatches, totalBatches);
        }

        JsonObject syncAllPayload = new JsonObject();
        syncAllPayload.addProperty("action", "sync_all");
        syncAllPayload.add("active_products", gson.toJsonTree(activeProducts));
        syncAllPayload.add("base_products", gson.toJsonTree(baseProducts));
        syncAllPayload.add("food_products", gson.toJsonTree(foodProducts));
        try {
            Call<JsonObject> call = api.syncAll(syncAllPayload);
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject data = response.body();
                if (data.get("success").getAsBoolean()) {
                    if (data.has("products")) {
                        JsonArray serverProducts = data.getAsJsonArray("products");
                        List<Product> merged = new ArrayList<>();
                        for (int i = 0; i < serverProducts.size(); i++) {
                            Product p = gson.fromJson(serverProducts.get(i), Product.class);
                            p.synced = 1;
                            merged.add(p);
                        }
                        db.replaceAllProducts(merged);
                    }
                    if (data.has("base_products")) {
                        JsonArray serverBase = data.getAsJsonArray("base_products");
                        List<BaseProduct> mergedBase = new ArrayList<>();
                        for (int i = 0; i < serverBase.size(); i++) {
                            mergedBase.add(gson.fromJson(serverBase.get(i), BaseProduct.class));
                        }
                        db.replaceAllBase(mergedBase);
                    }
                    if (data.has("food_products")) {
                        JsonArray serverFood = data.getAsJsonArray("food_products");
                        List<FoodProduct> mergedFood = new ArrayList<>();
                        for (int i = 0; i < serverFood.size(); i++) {
                            mergedFood.add(gson.fromJson(serverFood.get(i), FoodProduct.class));
                        }
                        db.replaceAllFood(mergedFood);
                    }

                    SharedPreferences syncPrefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE);
                    syncPrefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply();

                    Log.d(TAG, "Синхронизация успешно завершена");
                    return Result.success();
                } else {
                    Log.e(TAG, "Ошибка при финальной синхронизации: " + data.get("message").getAsString());
                    return Result.failure();
                }
            } else {
                Log.e(TAG, "Ошибка ответа при финальной синхронизации, код=" + response.code());
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "Исключение при финальной синхронизации", e);
            return Result.failure();
        }
    }

    private <T> List<List<T>> splitList(List<T> list, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return parts;
    }

    private boolean sendBatch(List<Product> batch, ApiService api, Gson gson) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "sync_batch");
        payload.add("type", gson.toJsonTree("active"));
        payload.add("items", gson.toJsonTree(batch));
        try {
            Call<JsonObject> call = api.syncBatch(payload);
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject data = response.body();
                if (data.get("success").getAsBoolean()) {
                    return true;
                } else {
                    Log.e(TAG, "Ошибка при отправке партии: " + data.get("message").getAsString());
                    return false;
                }
            } else {
                Log.e(TAG, "Сетевая ошибка при отправке партии");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Исключение при отправке партии", e);
            return false;
        }
    }

    private boolean sendBaseBatch(List<BaseProduct> batch, ApiService api, Gson gson) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "sync_batch");
        payload.add("type", gson.toJsonTree("base"));
        payload.add("items", gson.toJsonTree(batch));
        try {
            Call<JsonObject> call = api.syncBatch(payload);
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject data = response.body();
                return data.get("success").getAsBoolean();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Исключение при отправке партии base", e);
            return false;
        }
    }

    private boolean sendFoodBatch(List<FoodProduct> batch, ApiService api, Gson gson) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "sync_batch");
        payload.add("type", gson.toJsonTree("food"));
        payload.add("items", gson.toJsonTree(batch));
        try {
            Call<JsonObject> call = api.syncBatch(payload);
            Response<JsonObject> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                JsonObject data = response.body();
                return data.get("success").getAsBoolean();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Исключение при отправке партии food", e);
            return false;
        }
    }

    private void updateProgress(int total, int sent, int processedBatches, int totalBatches) {
        int progress = (int) ((double) sent / total * 100);
        setProgressAsync(new androidx.work.Data.Builder()
                .putInt("progress", progress)
                .putInt("total", total)
                .putInt("sent", sent)
                .putInt("processedBatches", processedBatches)
                .putInt("totalBatches", totalBatches)
                .build());
        Log.d(TAG, "Прогресс: " + progress + "% (" + sent + "/" + total + ")");
    }
}
