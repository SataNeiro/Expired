package com.expirytracker.fragments;
import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import android.widget.ImageView; import android.widget.TextView;
import androidx.annotation.NonNull; import androidx.recyclerview.widget.RecyclerView;
import com.expirytracker.R; import java.util.List;
public class SettingsListAdapter extends RecyclerView.Adapter<SettingsListAdapter.ViewHolder> {
    private List<SettingsItem> items; private OnItemClickListener listener;
    public interface OnItemClickListener { void onItemClick(SettingsItem item); }
    public SettingsListAdapter(List<SettingsItem> items, OnItemClickListener listener) { this.items = items; this.listener = listener; }
    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_list_item, parent, false);
        return new ViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SettingsItem item = items.get(position);
        holder.icon.setImageResource(item.iconRes);
        holder.title.setText(item.title);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(item); });
    }
    @Override public int getItemCount() { return items.size(); }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon; TextView title;
        ViewHolder(@NonNull View itemView) { super(itemView); icon = itemView.findViewById(R.id.iv_icon); title = itemView.findViewById(R.id.tv_title); }
    }
}
