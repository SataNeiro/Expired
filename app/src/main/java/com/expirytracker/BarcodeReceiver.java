package com.expirytracker;
import android.content.BroadcastReceiver; import android.content.Context; import android.content.Intent;
import android.content.SharedPreferences; import android.util.Log;
public class BarcodeReceiver extends BroadcastReceiver {
    private static final String TAG = "BarcodeReceiver"; private BarcodeListener listener;
    public interface BarcodeListener { void onBarcodeReceived(String barcode); }
    public BarcodeReceiver() {}
    public BarcodeReceiver(BarcodeListener listener) { this.listener = listener; }
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) { Log.w(TAG, "Intent is null"); return; }
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String expectedAction = prefs.getString("broadcast_action", "com.hht.scanwedge");
        String extraDataKey = prefs.getString("broadcast_extra_data", "com.hht.datawedge.data_string");
        if (expectedAction.equals(intent.getAction())) {
            String barcode = null;
            if (intent.hasExtra(extraDataKey)) barcode = intent.getStringExtra(extraDataKey);
            if (barcode == null || barcode.isEmpty()) {
                String[] fallbackKeys = {"data", "barcode", "com.symbol.datawedge.data_string", "com.datalogic.decode.DECODE_DATA", "scan_data", "BarcodeData"};
                for (String key : fallbackKeys) { if (intent.hasExtra(key)) { barcode = intent.getStringExtra(key); if (barcode != null && !barcode.isEmpty()) break; } }
            }
            if (barcode != null && !barcode.isEmpty()) {
                Log.d(TAG, "Barcode: " + barcode);
                if (listener != null) listener.onBarcodeReceived(barcode);
            } else { Log.w(TAG, "Barcode not found"); }
        }
    }
}
