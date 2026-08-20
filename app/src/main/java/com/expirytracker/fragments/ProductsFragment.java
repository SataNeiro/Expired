package com.expirytracker.fragments;
import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import androidx.annotation.NonNull; import androidx.fragment.app.Fragment; import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.expirytracker.MainActivity; import com.expirytracker.R; import com.expirytracker.models.Product;
import java.text.SimpleDateFormat; import java.util.ArrayList; import java.util.Collections; import java.util.Date; import java.util.List; import java.util.Locale;

public class ProductsFragment extends Fragment {
    private RecyclerView recyclerView; private ProductAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Product> products = new ArrayList<>();
    private boolean showArchived = false;
    private boolean showExpiredOnly = false;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products, container, false);
        recyclerView = view.findViewById(R.id.recycler_products);
        recyclerView.setHasFixedSize(true);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.app_green));

        String baseUrl = getContext().getSharedPreferences("settings", 0).getString("server_url", "http://192.168.0.191");
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        adapter = new ProductAdapter(getContext(), this, baseUrl);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).syncNow(() -> {
                    if (isAdded() && swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                        refreshList();
                    }
                });
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        refreshList();
        return view;
    }

    public void setShowArchived(boolean showArchived) { this.showArchived = showArchived; refreshList(); }
    public void setShowExpiredOnly(boolean showExpiredOnly) { this.showExpiredOnly = showExpiredOnly; refreshList(); }

    public void refreshList() {
        if (getActivity() instanceof MainActivity) {
            products.clear();
            List<Product> source;
            if (showArchived) source = ((MainActivity) getActivity()).getAllProducts();
            else source = ((MainActivity) getActivity()).getProducts();
            if (showExpiredOnly) {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                for (Product p : source) {
                    if (p.expiry != null && p.expiry.compareTo(today) < 0) {
                        products.add(p);
                    }
                }
            } else {
                products.addAll(source);
            }
            sortProducts();
            adapter.updateProducts(products);
        }
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void sortProducts() {
        Collections.sort(products, (p1, p2) -> {
            String status1 = getStatus(p1.expiry);
            String status2 = getStatus(p2.expiry);
            int priority1 = "expired".equals(status1) ? 0 : "warning".equals(status1) ? 1 : 2;
            int priority2 = "expired".equals(status2) ? 0 : "warning".equals(status2) ? 1 : 2;
            if (priority1 != priority2) return Integer.compare(priority1, priority2);
            String d1 = p1.expiry != null ? p1.expiry : "9999-12-31";
            String d2 = p2.expiry != null ? p2.expiry : "9999-12-31";
            return d1.compareTo(d2);
        });
    }

    private String getStatus(String expiry) {
        if (expiry == null || expiry.isEmpty()) return "ok";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long days = (sdf.parse(expiry).getTime() - new Date().getTime()) / (24 * 60 * 60 * 1000);
            if (days < 0) return "expired";
            if (days <= 30) return "warning";
            return "ok";
        } catch (Exception e) { return "ok"; }
    }
}
