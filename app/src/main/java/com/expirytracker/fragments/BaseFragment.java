package com.expirytracker.fragments;
import android.os.Bundle; import android.view.LayoutInflater; import android.view.View; import android.view.ViewGroup;
import androidx.annotation.NonNull; import androidx.fragment.app.Fragment; import androidx.recyclerview.widget.RecyclerView;
import com.expirytracker.MainActivity; import com.expirytracker.R; import com.expirytracker.models.BaseProduct;

public class BaseFragment extends Fragment implements BaseAdapter.OnBaseClickListener {
    private RecyclerView recyclerView; private BaseAdapter adapter;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base, container, false);
        recyclerView = view.findViewById(R.id.recycler_base);
        recyclerView.setHasFixedSize(true);

        String baseUrl = getContext().getSharedPreferences("settings", 0).getString("server_url", "http://192.168.0.191");
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        adapter = new BaseAdapter(getContext(), this, baseUrl);
        recyclerView.setAdapter(adapter);
        refreshBase();
        return view;
    }

    public void refreshBase() {
        if (getActivity() instanceof MainActivity) {
            adapter.updateBase(((MainActivity) getActivity()).getBaseProducts());
        }
    }

    @Override public void onUseBase(BaseProduct product) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).fillFormFromBase(product);
        }
    }

    @Override public void onDeleteBase(BaseProduct product) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).deleteBaseProduct(product.baseId);
            refreshBase();
        }
    }
}
