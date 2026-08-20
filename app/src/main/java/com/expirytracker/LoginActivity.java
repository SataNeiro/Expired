package com.expirytracker;
import android.content.Intent; import android.content.SharedPreferences; import android.os.Bundle;
import android.view.View; import android.widget.Button; import android.widget.CheckBox; import android.widget.EditText;
import android.widget.TextView; import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.expirytracker.network.ApiService;
import com.expirytracker.network.RetrofitClient;
import com.google.gson.JsonObject;
import retrofit2.Call; import retrofit2.Callback; import retrofit2.Response;
public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword; private CheckBox cbRemember;
    private Button btnLogin, btnRegister; private TextView tvSwitch; private boolean isLoginMode = true;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.et_username); etPassword = findViewById(R.id.et_password);
        cbRemember = findViewById(R.id.cb_remember); btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register); tvSwitch = findViewById(R.id.tv_switch_mode);
        RetrofitClient.init(this);
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        if (prefs.getBoolean("logged_in", false)) { startMainActivity(); return; }
        String savedUser = prefs.getString("saved_username", ""); String savedPass = prefs.getString("saved_password", "");
        boolean isRemembered = prefs.getBoolean("remember_me", false);
        if (isRemembered && !savedUser.isEmpty()) { etUsername.setText(savedUser); etPassword.setText(savedPass); cbRemember.setChecked(true); }
        btnLogin.setOnClickListener(v -> login()); btnRegister.setOnClickListener(v -> register());
        tvSwitch.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) { btnLogin.setVisibility(View.VISIBLE); btnRegister.setVisibility(View.GONE); tvSwitch.setText("Нет аккаунта? Зарегистрироваться"); }
            else { btnLogin.setVisibility(View.GONE); btnRegister.setVisibility(View.VISIBLE); tvSwitch.setText("Уже есть аккаунт? Войти"); }
        });
    }
    private void login() {
        String username = etUsername.getText().toString().trim(); String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return; }
        JsonObject body = new JsonObject(); body.addProperty("action", "login"); body.addProperty("username", username); body.addProperty("password", password);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.login(body).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.get("success").getAsBoolean()) {
                        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("logged_in", true).putString("username", username);
                        if (cbRemember.isChecked()) { editor.putBoolean("remember_me", true).putString("saved_username", username).putString("saved_password", password); }
                        else { editor.putBoolean("remember_me", false).remove("saved_username").remove("saved_password"); }
                        editor.apply(); startMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, data.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                } else { Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) { Toast.makeText(LoginActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }
    private void register() {
        String username = etUsername.getText().toString().trim(); String password = etPassword.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) { Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show(); return; }
        JsonObject body = new JsonObject(); body.addProperty("action", "register"); body.addProperty("username", username); body.addProperty("password", password);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.register(body).enqueue(new Callback<JsonObject>() {
            @Override public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.get("success").getAsBoolean()) {
                        isLoginMode = true; btnLogin.setVisibility(View.VISIBLE); btnRegister.setVisibility(View.GONE);
                        tvSwitch.setText("Нет аккаунта? Зарегистрироваться"); etUsername.setText(""); etPassword.setText("");
                        Toast.makeText(LoginActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    } else { Toast.makeText(LoginActivity.this, data.get("message").getAsString(), Toast.LENGTH_SHORT).show(); }
                } else { Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show(); }
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) { Toast.makeText(LoginActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show(); }
        });
    }
    private void startMainActivity() { Intent intent = new Intent(this, MainActivity.class); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(intent); finish(); }
}
