package com.expirytracker.fragments;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.expirytracker.R;
import com.expirytracker.models.BaseProduct;
import java.util.ArrayList;
import java.util.List;

public class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.BaseViewHolder> {
    private final Context context;
    private List<BaseProduct> baseList = new ArrayList<>();
    private final OnBaseClickListener listener;
    private final String baseUrl;

    public interface OnBaseClickListener { void onUseBase(BaseProduct product); void onDeleteBase(BaseProduct product); }

    public BaseAdapter(Context context, OnBaseClickListener listener, String baseUrl) {
        this.context = context; this.listener = listener; this.baseUrl = baseUrl;
    }

    @NonNull @Override public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.base_item, parent, false);
        return new BaseViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        BaseProduct bp = baseList.get(position);
        holder.tvName.setText(bp.name != null ? bp.name : "Без названия");
        holder.tvBarcode.setText("ШК " + (bp.barcode != null ? bp.barcode : "-"));

        String image = bp.image;
        if (image != null && !image.isEmpty()) {
            if (isBase64(image)) {
                try {
                    byte[] decoded = Base64.decode(image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    if (bitmap != null) holder.ivImage.setImageBitmap(bitmap);
                    else holder.ivImage.setImageResource(R.drawable.ic_launcher);
                } catch (Exception e) {
                    holder.ivImage.setImageResource(R.drawable.ic_launcher);
                }
            } else {
                String fullImageUrl = getFullImageUrl(image);
                if (fullImageUrl != null && (fullImageUrl.startsWith("http://") || fullImageUrl.startsWith("https://"))) {
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
                } else {
                    holder.ivImage.setImageResource(R.drawable.ic_launcher);
                }
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher);
        }

        holder.btnUse.setOnClickListener(v -> { if (listener != null) listener.onUseBase(bp); });
        holder.itemView.setOnLongClickListener(v -> { if (listener != null) listener.onDeleteBase(bp); return true; });
    }

    @Override public int getItemCount() { return baseList.size(); }
    public void updateBase(List<BaseProduct> newList) { this.baseList = new ArrayList<>(newList); notifyDataSetChanged(); }

    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) return false;
        if (str.startsWith("http://") || str.startsWith("https://") || str.startsWith("uploads/")) return false;
        return str.matches("^[A-Za-z0-9+/]+=*$");
    }

    private String getFullImageUrl(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        path = path.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        return cleanBase + cleanPath;
    }

    static class BaseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage; TextView tvName, tvBarcode; ImageButton btnUse;
        BaseViewHolder(@NonNull View itemView) { super(itemView);
            ivImage = itemView.findViewById(R.id.iv_base_image);
            tvName = itemView.findViewById(R.id.tv_base_name);
            tvBarcode = itemView.findViewById(R.id.tv_base_barcode);
            btnUse = itemView.findViewById(R.id.btn_use);
        }
    }
}
