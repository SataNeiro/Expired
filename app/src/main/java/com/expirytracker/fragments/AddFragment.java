package com.expirytracker.fragments;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.expirytracker.MainActivity;
import com.expirytracker.R;
import com.expirytracker.db.ExpiryDbHelper;
import com.expirytracker.models.BaseProduct;
import com.expirytracker.models.FoodProduct;
import com.expirytracker.models.Product;
import com.expirytracker.network.ApiService;
import com.expirytracker.network.RetrofitClient;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddFragment extends Fragment implements MainActivity.BarcodeListener {
    private EditText etBarcode, etName, etQty, etExpiry;
    private ImageView ivProductPreview;
    private Button btnClear, btnSave;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedImageBase64 = null;
    private String existingImageUrl = null;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private SharedPreferences settingsPrefs;
    private boolean offlineMode;
    private String currentBaseId = null;
    private String currentFoodId = null;
    private boolean isUpdating = false;
    private ExpiryDbHelper dbHelper;
    private FoodProduct currentFoodItemFromBase = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && isAdded() && getContext() != null) {
                            try {
                                InputStream imageStream = getContext().getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                                if (bitmap != null) {
                                    int maxSize = 300;
                                    int width = bitmap.getWidth();
                                    int height = bitmap.getHeight();
                                    float scale = Math.min((float) maxSize / width, (float) maxSize / height);
                                    int newWidth = Math.round(width * scale);
                                    int newHeight = Math.round(height * scale);
                                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                                    byte[] byteArray = baos.toByteArray();
                                    selectedImageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
                                    if (ivProductPreview != null) ivProductPreview.setImageBitmap(scaledBitmap);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        etBarcode = view.findViewById(R.id.et_barcode);
        etName = view.findViewById(R.id.et_name);
        etQty = view.findViewById(R.id.et_qty);
        etExpiry = view.findViewById(R.id.et_expiry);
        ivProductPreview = view.findViewById(R.id.iv_product_preview);
        btnClear = view.findViewById(R.id.btn_clear);
        btnSave = view.findViewById(R.id.btn_save);

        dbHelper = new ExpiryDbHelper(requireContext());
        settingsPrefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        offlineMode = settingsPrefs.getBoolean("offline_mode", false);

        etBarcode.setEnabled(true);
        etBarcode.setFocusable(false);
        etBarcode.setClickable(false);
        etBarcode.setInputType(android.text.InputType.TYPE_NULL);
        etBarcode.setAlpha(0.8f);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBarcodeListener(this);
        }

        ivProductPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        etExpiry.setOnClickListener(v -> showDatePickerDialog());

        etBarcode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                if (!isAdded() || getContext() == null) return;
                String barcode = s.toString().trim();
                if (!barcode.isEmpty()) {
                    autoFillFromBarcode(barcode);
                }
            }
        });

        Button btnPlus = view.findViewById(R.id.btn_plus);
        Button btnMinus = view.findViewById(R.id.btn_minus);
        btnPlus.setOnClickListener(v -> {
            String current = etQty.getText().toString().trim();
            int value = current.isEmpty() ? 0 : Integer.parseInt(current);
            etQty.setText(String.valueOf(value + 1));
            etQty.setSelection(etQty.getText().length());
        });
        btnMinus.setOnClickListener(v -> {
            String current = etQty.getText().toString().trim();
            int value = current.isEmpty() ? 0 : Integer.parseInt(current);
            if (value > 0) {
                etQty.setText(String.valueOf(value - 1));
                etQty.setSelection(etQty.getText().length());
            }
        });

        btnClear.setOnClickListener(v -> clearFields());
        btnSave.setOnClickListener(v -> saveProduct());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBarcodeListener(null);
        }
    }

    @Override
    public void onBarcode(String barcode) {
        if (barcode != null && !barcode.isEmpty() && isAdded()) {
            isUpdating = true;
            etBarcode.setText(barcode);
            isUpdating = false;
            autoFillFromBarcode(barcode);
        }
    }

    private void clearFields() {
        if (!isAdded() || getContext() == null) return;
        isUpdating = true;
        etBarcode.setText("");
        etName.setText("");
        etQty.setText("");
        etExpiry.setText("");
        isUpdating = false;
        ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
        selectedImageBase64 = null;
        existingImageUrl = null;
        currentBaseId = null;
        currentFoodId = null;
        currentFoodItemFromBase = null;
        selectedDate = Calendar.getInstance();
    }

    private void showDatePickerDialog() {
        if (!isAdded() || getContext() == null) return;
        String currentDate = etExpiry.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                selectedDate.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(currentDate));
            } catch (Exception ignored) {}
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    isUpdating = true;
                    etExpiry.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.getTime()));
                    isUpdating = false;
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // =============================================================
    //  НОВАЯ ЛОГИКА АВТОЗАПОЛНЕНИЯ (с запросом к серверу при наличии сети)
    // =============================================================
    private void autoFillFromBarcode(String barcode) {
        if (!isAdded() || getContext() == null) return;
        if (barcode == null || barcode.isEmpty()) return;

        // Если есть интернет – всегда идём на сервер за свежими данными
        if (!offlineMode && isNetworkAvailable()) {
            loadProductFromServer(barcode);
            return;
        }

        // Офлайн или нет сети – используем локальную БД
        FoodProduct food = dbHelper.getFoodByBarcode(barcode);
        if (food != null) {
            fillFromFood(food);
            return;
        }
        BaseProduct base = dbHelper.getBaseByBarcode(barcode);
        if (base != null) {
            fillFromBase(base);
        } else {
            Toast.makeText(getContext(), "Товар не найден в локальной базе", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProductFromServer(String barcode) {
        ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("Загрузка данных с сервера...");
        pd.setCancelable(false);
        pd.show();

        JsonObject body = new JsonObject();
        body.addProperty("action", "get_product_by_barcode");
        body.addProperty("barcode", barcode);

        ApiService api = RetrofitClient.getClient(requireContext()).create(ApiService.class);
        api.getProductByBarcode(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                pd.dismiss();
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.get("success").getAsBoolean()) {
                        String source = data.get("source").getAsString();
                        JsonObject item = data.getAsJsonObject("data");

                        if ("food".equals(source)) {
                            FoodProduct fp = new FoodProduct();
                            fp.foodId = item.get("food_id").getAsString();
                            fp.name = item.get("name").getAsString();
                            fp.barcode = item.get("barcode").getAsString();
                            fp.image = item.get("image").isJsonNull() ? null : item.get("image").getAsString();
                            fp.imageOriginal = item.get("imageOriginal").isJsonNull() ? null : item.get("imageOriginal").getAsString();
                            // Обновляем локальную БД
                            dbHelper.insertOrUpdateFood(fp);
                            fillFromFood(fp);
                        } else if ("base".equals(source)) {
                            BaseProduct bp = new BaseProduct();
                            bp.baseId = item.get("base_id").getAsString();
                            bp.name = item.get("name").getAsString();
                            bp.barcode = item.get("barcode").getAsString();
                            bp.image = item.get("image").isJsonNull() ? null : item.get("image").getAsString();
                            bp.imageOriginal = item.get("imageOriginal").isJsonNull() ? null : item.get("imageOriginal").getAsString();
                            dbHelper.insertOrUpdateBase(bp);
                            fillFromBase(bp);
                        }
                    } else {
                        // На сервере не найдено – пробуем локально
                        FoodProduct food = dbHelper.getFoodByBarcode(barcode);
                        if (food != null) fillFromFood(food);
                        else {
                            BaseProduct base = dbHelper.getBaseByBarcode(barcode);
                            if (base != null) fillFromBase(base);
                            else Toast.makeText(getContext(), "Товар не найден", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    // Ошибка ответа – fallback на локальную БД
                    FoodProduct food = dbHelper.getFoodByBarcode(barcode);
                    if (food != null) fillFromFood(food);
                    else {
                        BaseProduct base = dbHelper.getBaseByBarcode(barcode);
                        if (base != null) fillFromBase(base);
                        else Toast.makeText(getContext(), "Ошибка сервера и нет локальной копии", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                pd.dismiss();
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), "Ошибка сети, загружено из кэша", Toast.LENGTH_SHORT).show();
                // Fallback на локальную БД
                FoodProduct food = dbHelper.getFoodByBarcode(barcode);
                if (food != null) fillFromFood(food);
                else {
                    BaseProduct base = dbHelper.getBaseByBarcode(barcode);
                    if (base != null) fillFromBase(base);
                    else Toast.makeText(getContext(), "Ошибка сети и нет локальной копии", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Проверка наличия интернета
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    // =============================================================
    //  ЗАПОЛНЕНИЕ ПОЛЕЙ
    // =============================================================
    public void fillFromFood(FoodProduct fp) {
        if (!isAdded() || getContext() == null) return;
        isUpdating = true;
        etName.setText(fp.name != null ? fp.name : "");
        etBarcode.setText(fp.barcode != null ? fp.barcode : "");
        etQty.setText("");
        etExpiry.setText("");
        isUpdating = false;
        currentFoodItemFromBase = fp;
        currentFoodId = fp.foodId;
        currentBaseId = null;
        loadImage(fp.image);
    }

    public void fillFromBase(BaseProduct bp) {
        if (!isAdded() || getContext() == null) return;
        isUpdating = true;
        etName.setText(bp.name != null ? bp.name : "");
        etBarcode.setText(bp.barcode != null ? bp.barcode : "");
        etQty.setText("");
        etExpiry.setText("");
        isUpdating = false;
        currentBaseId = bp.baseId;
        currentFoodId = null;
        currentFoodItemFromBase = null;
        loadImage(bp.image);
    }

    private void loadImage(String imagePath) {
        if (!isAdded() || getContext() == null) return;
        if (imagePath != null && !imagePath.isEmpty()) {
            if (isBase64(imagePath)) {
                try {
                    byte[] decoded = Base64.decode(imagePath, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bitmap != null) {
                        ivProductPreview.setImageBitmap(bitmap);
                    } else {
                        ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
                    }
                } catch (Exception e) {
                    ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                String fullUrl = getFullImageUrl(imagePath);
                if (fullUrl != null) {
                    try {
                        // Принудительно сбрасываем кэш для этого конкретного изображения
                        Glide.with(this)
                                .load(fullUrl)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder)
                                .into(ivProductPreview);
                    } catch (Exception e) {
                        ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
                    }
                } else {
                    ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
                }
            }
        } else {
            ivProductPreview.setImageResource(R.drawable.ic_image_placeholder);
        }
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("uploads/")) return false;
        return str.matches("^[A-Za-z0-9+/]+=*$");
    }

    private String getFullImageUrl(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        path = path.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String baseUrl = requireContext().getSharedPreferences("settings", 0)
                .getString("server_url", "http://192.168.0.191");
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return baseUrl + cleanPath;
    }

    // =============================================================
    //  СОХРАНЕНИЕ ТОВАРА (без изменений)
    // =============================================================
    private void saveProduct() {
        if (!isAdded() || getContext() == null) return;
        String barcode = etBarcode.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String qtyStr = etQty.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        if (name.isEmpty() || qtyStr.isEmpty() || expiry.isEmpty()) {
            Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }
        int qty = Integer.parseInt(qtyStr);
        String imageToSave = (selectedImageBase64 != null) ? selectedImageBase64 : existingImageUrl;

        // Сохраняем/обновляем в foodbase
        FoodProduct foodItem;
        if (currentFoodItemFromBase != null) {
            foodItem = currentFoodItemFromBase;
            if (!name.equals(foodItem.name)) foodItem.name = name;
            if (imageToSave != null && !imageToSave.equals(foodItem.image)) foodItem.image = imageToSave;
            dbHelper.insertOrUpdateFood(foodItem);
        } else {
            foodItem = new FoodProduct();
            foodItem.foodId = String.valueOf(System.currentTimeMillis());
            foodItem.name = name;
            foodItem.barcode = barcode;
            foodItem.image = imageToSave;
            foodItem.imageOriginal = imageToSave;
            dbHelper.insertOrUpdateFood(foodItem);
            currentFoodItemFromBase = foodItem;
        }

        // Сохраняем активный продукт
        Product product = new Product();
        product.id = String.valueOf(System.currentTimeMillis()) + "_" + (int)(Math.random() * 1000);
        product.name = name;
        product.qty = qty;
        product.barcode = barcode;
        product.expiry = expiry;
        product.status = "ok";
        product.archived = false;
        product.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date());
        product.synced = 0;
        product.image = imageToSave;
        product.imageOriginal = imageToSave;
        dbHelper.insertOrUpdate(product);

        if (getActivity() instanceof MainActivity) {
            MainActivity act = (MainActivity) getActivity();
            act.loadProductsFromDB();
            act.loadBaseFromDB();
            act.loadFoodFromDB();
            act.refreshProductsList();
            act.refreshBaseList();
            if (!offlineMode) act.syncNow();
        }

        String savedBarcode = barcode;
        clearFields();
        isUpdating = true;
        etBarcode.setText(savedBarcode);
        isUpdating = false;
        autoFillFromBarcode(savedBarcode);
    }
}