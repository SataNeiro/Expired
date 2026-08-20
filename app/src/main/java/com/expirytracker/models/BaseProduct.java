package com.expirytracker.models;
import com.google.gson.annotations.SerializedName;
public class BaseProduct {
    @SerializedName("base_id") public String baseId;
    @SerializedName("name") public String name;
    @SerializedName("barcode") public String barcode;
    @SerializedName("image") public String image;
    @SerializedName("imageOriginal") public String imageOriginal;
    public BaseProduct() {}
}
