package com.expirytracker.network;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
public class PersistentCookieJar implements CookieJar {
    private static final String TAG = "CookieJar";
    private static final String PREF_NAME = "cookies_store";
    private final SharedPreferences prefs;
    public PersistentCookieJar(Context context) { this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); }
    @Override public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        if (cookies.isEmpty()) return;
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        for (Cookie cookie : cookies) {
            sb.append(cookie.name()).append("=").append(cookie.value())
              .append(";").append(cookie.domain())
              .append(";").append(cookie.path()).append("|");
        }
        editor.putString(url.host(), sb.toString()).apply();
        Log.d(TAG, "Saved cookies: " + sb);
    }
    @Override public List<Cookie> loadForRequest(HttpUrl url) {
        String cookieString = prefs.getString(url.host(), null);
        List<Cookie> cookies = new ArrayList<>();
        if (cookieString == null || cookieString.isEmpty()) return cookies;
        String[] parts = cookieString.split("\\|");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String[] attr = part.split(";");
            if (attr.length < 3) continue;
            String[] nameValue = attr[0].split("=");
            if (nameValue.length < 2) continue;
            Cookie.Builder builder = new Cookie.Builder()
                    .name(nameValue[0])
                    .value(nameValue[1])
                    .domain(attr[1])
                    .path(attr[2]);
            cookies.add(builder.build());
        }
        Log.d(TAG, "Loaded cookies: " + cookies.size());
        return cookies;
    }
}
