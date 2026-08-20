package com.expirytracker.network;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api.php") Call<JsonObject> login(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> register(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> listProducts(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> listBaseProducts(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> listFoodProducts(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> addProduct(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> deleteProduct(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> saveProducts(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> uploadImage(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> syncAll(@Body JsonObject body);
    @POST("api.php") Call<JsonObject> syncBatch(@Body JsonObject body);

    // НОВЫЙ МЕТОД
    @POST("api.php") Call<JsonObject> getProductByBarcode(@Body JsonObject body);
}