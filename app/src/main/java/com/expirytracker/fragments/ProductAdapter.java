package com.expirytracker.fragments;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.expirytracker.MainActivity;
import com.expirytracker.R;
import com.expirytracker.models.Product;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private final Context context;
    private List<Product> products = new ArrayList<>();
    private final ProductsFragment fragment;
    private final LruCache<String, Bitmap> bitmapCache;
    private final String baseUrl;

    public ProductAdapter(Context context, ProductsFragment fragment, String baseUrl) {
        this.context = context; this.fragment = fragment; this.baseUrl = baseUrl;
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount() / 1024; }
        };
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = products.get(position);
        holder.tvName.setText(p.name != null ? p.name : "Без названия");
        holder.tvBarcode.setText("ШК " + (p.barcode != null ? p.barcode : "-"));
        String doo = formatDate(p.expiry);
        holder.tvDates.setText("Годен до: " + doo);
        holder.tvQty.setText(String.valueOf(p.qty));
        holder.tvQty.setTextColor(ContextCompat.getColor(context, p.qty < 10 ? R.color.quantity_yellow : R.color.quantity_green));
        holder.tvUnit.setText("шт");

        int daysLeft = calculateDaysLeft(p.expiry);
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int warningDays = prefs.getInt("warning_days", 7);
        int criticalDays = prefs.getInt("critical_days", 0);
        View indicator = holder.itemView.findViewById(R.id.view_indicator);
        if (daysLeft > warningDays) indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.quantity_green));
        else if (daysLeft > criticalDays) indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.quantity_yellow));
        else indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.server_disconnected));

        String image = p.image;
        if (image != null && !image.isEmpty()) {
            if (isBase64(image)) {
                Bitmap cached = bitmapCache.get(image);
                if (cached != null) {
                    holder.ivImage.setImageBitmap(cached);
                } else {
                    try {
                        byte[] decoded = Base64.decode(image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        if (bitmap != null) {
                            bitmapCache.put(image, bitmap);
                            holder.ivImage.setImageBitmap(bitmap);
                        } else {
                            holder.ivImage.setImageResource(R.drawable.ic_launcher);
                        }
                    } catch (Exception e) {
                        holder.ivImage.setImageResource(R.drawable.ic_launcher);
                    }
                }
                holder.ivImage.setOnClickListener(null);
            } else {
                String fullImageUrl = getFullImageUrl(image);
                if (fullImageUrl != null && (fullImageUrl.startsWith("http://") || fullImageUrl.startsWith("https://"))) {
                    if (context instanceof Activity && !((Activity) context).isFinishing()) {
                        try {
                            Glide.with(holder.itemView.getContext())
                                 .load(fullImageUrl)
                                 .placeholder(R.drawable.ic_launcher)
                                 .error(R.drawable.ic_launcher)
                                 .diskCacheStrategy(DiskCacheStrategy.ALL)
                                 .into(holder.ivImage);
                        } catch (Exception e) {
                            holder.ivImage.setImageResource(R.drawable.ic_launcher);
                        }
                    }
                    holder.ivImage.setOnClickListener(v -> {
                        if (context instanceof Activity && !((Activity) context).isFinishing()) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullImageUrl));
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                context.startActivity(intent);
                            } catch (Exception ignored) {}
                        }
                    });
                } else {
                    holder.ivImage.setImageResource(R.drawable.ic_launcher);
                    holder.ivImage.setOnClickListener(null);
                }
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher);
            holder.ivImage.setOnClickListener(null);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (fragment.getActivity() instanceof MainActivity) {
                ((MainActivity) fragment.getActivity()).deleteProductFromDB(p.id);
            }
        });
    }

    @Override public int getItemCount() { return products.size(); }

    public void updateProducts(List<Product> newProducts) {
        final List<Product> oldList = new ArrayList<>(this.products);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldList.size(); }
            @Override public int getNewListSize() { return newProducts.size(); }
            @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = oldList.get(oldItemPosition).id;
                String newId = newProducts.get(newItemPosition).id;
                return oldId == null ? newId == null : oldId.equals(newId);
            }
            @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Product oldP = oldList.get(oldItemPosition);
                Product newP = newProducts.get(newItemPosition);
                boolean nameEq = (oldP.name == null) ? (newP.name == null) : oldP.name.equals(newP.name);
                boolean barcodeEq = (oldP.barcode == null) ? (newP.barcode == null) : oldP.barcode.equals(newP.barcode);
                boolean expiryEq = (oldP.expiry == null) ? (newP.expiry == null) : oldP.expiry.equals(newP.expiry);
                boolean statusEq = (oldP.status == null) ? (newP.status == null) : oldP.status.equals(newP.status);
                boolean createdAtEq = (oldP.createdAt == null) ? (newP.createdAt == null) : oldP.createdAt.equals(newP.createdAt);
                return nameEq && oldP.qty == newP.qty && barcodeEq && expiryEq && statusEq &&
                       oldP.archived == newP.archived && createdAtEq && oldP.synced == newP.synced;
            }
        });
        this.products = new ArrayList<>(newProducts);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("uploads/")) return false;
        return str.matches("^[A-Za-z0-9+/]+=*$");
    }

    private int calculateDaysLeft(String expiry) {
        if (expiry == null || expiry.isEmpty()) return Integer.MAX_VALUE;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date expiryDate = sdf.parse(expiry);
            Date today = new Date();
            long diff = expiryDate.getTime() - today.getTime();
            return (int) (diff / (24 * 60 * 60 * 1000));
        } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            String[] parts = dateStr.split("-");
            if (parts.length == 3) return parts[2] + "." + parts[1] + "." + parts[0].substring(2);
        } catch (Exception ignored) {}
        return "";
    }

    private String getFullImageUrl(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        path = path.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return cleanBase + cleanPath;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage; TextView tvName, tvDates, tvBarcode, tvQty, tvUnit; ImageView btnDelete;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_product_image);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDates = itemView.findViewById(R.id.tv_dates);
            tvBarcode = itemView.findViewById(R.id.tv_barcode);
            tvQty = itemView.findViewById(R.id.tv_qty);
            tvUnit = itemView.findViewById(R.id.tv_unit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
