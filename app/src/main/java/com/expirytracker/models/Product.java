package com.expirytracker.models;
import com.google.gson.annotations.SerializedName;
public class Product {
    @SerializedName("id") public String id;
    @SerializedName("name") public String name;
    @SerializedName("qty") public int qty;
    @SerializedName("barcode") public String barcode;
    @SerializedName("expiry") public String expiry;
    @SerializedName("image") public String image;
    @SerializedName("imageOriginal") public String imageOriginal;
    @SerializedName("status") public String status;
    @SerializedName("archived") public boolean archived;
    @SerializedName("createdAt") public String createdAt;
    public int synced = 1;
    public Product() {}
}
