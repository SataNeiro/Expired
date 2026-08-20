package com.expirytracker.models;
import com.google.gson.annotations.SerializedName;
public class FoodProduct {
    @SerializedName("food_id") public String foodId;
    @SerializedName("name") public String name;
    @SerializedName("barcode") public String barcode;
    @SerializedName("image") public String image;
    @SerializedName("imageOriginal") public String imageOriginal;
    public FoodProduct() {}
}
