// File: app/src/main/java/com/ganshapebattle/LoginActivity.java
package com.ganshapebattle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "AuthSignIn";

    // ==== CẤU HÌNH SUPABASE CỦA BẠN ====
    private static final String SUPABASE_URL = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";
    private static final String REDIRECT_URI = "com.ganshapebattle://oauth-callback";

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView gotoRegister, forgotPassword;
    private ImageView googleLogin;

    private UserService userService;
    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userService = new UserService();

        inputEmail     = findViewById(R.id.inputEmail);
        inputPassword  = findViewById(R.id.inputPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        gotoRegister   = findViewById(R.id.gotoRegister);
        forgotPassword = findViewById(R.id.forgotPassword);
        googleLogin    = findViewById(R.id.googleLogin);

        btnLogin.setOnClickListener(v -> performLogin());
        gotoRegister.setOnClickListener(v -> startActivity(new Intent(this, Register.class)));
        forgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPassword.class)));
        googleLogin.setOnClickListener(v -> signInWithGoogleViaSupabase());

        // Nếu app được mở do redirect OAuth trong khi đang chạy
        handleSupabaseRedirect(getIntent());
    }

    // ================= Email/Password =================
    private void performLogin() {
        final String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loginUser(email, password, new SupabaseCallback<String>() {
            @Override public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                    fetchUsernameAndNavigate(email);
                });
            }
            @Override public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Login failed", e);
                    Toast.makeText(LoginActivity.this,
                            "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ================= Google Sign-In via Supabase (Web Redirect) =================
    private void signInWithGoogleViaSupabase() {
        Uri authUri = Uri.parse(SUPABASE_URL + "/auth/v1/authorize")
                .buildUpon()
                .appendQueryParameter("provider", "google")
                .appendQueryParameter("redirect_to", REDIRECT_URI)
                .build();

        new CustomTabsIntent.Builder().build().launchUrl(this, authUri);
    }

    @Override
    protected void onNewIntent(@Nullable Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            setIntent(intent);
            handleSupabaseRedirect(intent);
        }
    }

    /** Parse fragment #access_token=...&refresh_token=... từ redirect và xử lý đăng nhập */
    private void handleSupabaseRedirect(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;

        String fragment = data.getEncodedFragment(); // Supabase trả qua fragment (#...)
        if (fragment == null || fragment.isEmpty()) return;

        Map<String, String> q = parseFragment(fragment);
        String accessToken  = q.get("access_token");
        String refreshToken = q.get("refresh_token"); // (nếu muốn lưu)

        if (accessToken == null) {
            Log.w(TAG, "OAuth callback không có access_token");
            return;
        }

        // Lấy thông tin user từ Supabase bằng access_token
        fetchSupabaseUser(accessToken, new SupabaseCallback<Map<String, Object>>() {
            @Override public void onSuccess(Map<String, Object> userMap) {
                runOnUiThread(() -> {
                    try {
                        String email = (String) userMap.get("email");
                        Map<String, Object> meta = (Map<String, Object>) userMap.get("user_metadata");
                        String avatarUrl = meta != null ? (String) meta.get("avatar_url") : null;
                        String displayName = meta != null ? (String) meta.get("full_name") : null;
                        String usernameForCheck = (displayName != null && !displayName.isEmpty())
                                ? displayName
                                : (email != null && email.contains("@") ? email.split("@")[0] : "User");

                        if (email == null) {
                            Toast.makeText(LoginActivity.this, "Không lấy được email từ Supabase", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(LoginActivity.this,
                                "Đăng nhập Google thành công với " + email, Toast.LENGTH_SHORT).show();
                        String role = determineRole(email);
                        checkIfPasswordExists(email, accessToken, role, avatarUrl, usernameForCheck);
                    } catch (Exception e) {
                        Log.e(TAG, "Parse user after OAuth error", e);
                        Toast.makeText(LoginActivity.this,
                                "Đăng nhập thành công nhưng lỗi xử lý dữ liệu.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Fetch user after OAuth failed", e);
                    Toast.makeText(LoginActivity.this,
                            "Không đọc được thông tin người dùng.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private Map<String, String> parseFragment(String fragment) {
        Map<String, String> map = new HashMap<>();
        String[] parts = fragment.split("&");
        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                map.put(Uri.decode(kv[0]), Uri.decode(kv[1]));
            }
        }
        return map;
    }

    /** Gọi Supabase /auth/v1/user với Bearer access_token để lấy email + metadata */
    private void fetchSupabaseUser(String accessToken, SupabaseCallback<Map<String, Object>> cb) {
        Request req = new Request.Builder()
                .url(SUPABASE_URL + "/auth/v1/user")
                .headers(new Headers.Builder()
                        .add("Authorization", "Bearer " + accessToken)
                        .add("apikey", SUPABASE_ANON_KEY)
                        .build())
                .get()
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onFailure(e);
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful()) {
                    cb.onFailure(new IOException("HTTP " + resp.code()));
                    return;
                }
                try {
                    String body = resp.body().string();
                    JSONObject json = new JSONObject(body);
                    Map<String, Object> map = new HashMap<>();
                    map.put("email", json.optString("email", null));
                    JSONObject meta = json.optJSONObject("user_metadata");
                    Map<String, Object> userMeta = new HashMap<>();
                    if (meta != null) {
                        userMeta.put("avatar_url", meta.optString("avatar_url", null));
                        userMeta.put("full_name", meta.optString("full_name", null));
                    }
                    map.put("user_metadata", userMeta);
                    cb.onSuccess(map);
                } catch (Exception ex) {
                    cb.onFailure(ex);
                }
            }
        });
    }

    // ================= các hàm giữ nguyên logic của bạn =================
    private void checkIfPasswordExists(final String email,
                                       String accessToken,
                                       String role,
                                       String avatarUrl,
                                       String username) {
        userService.getUserByEmail(email, new SupabaseCallback<User>() {
            @Override public void onSuccess(User user) {
                runOnUiThread(() -> {
                    String finalUsername = (user != null && user.getUsername() != null && !user.getUsername().isEmpty())
                            ? user.getUsername() : username;
                    if (user != null && user.getPassword() != null && !user.getPassword().isEmpty()) {
                        Log.d(TAG, "User " + email + " đã có mật khẩu. Chuyển đến MainActivity.");
                        navigateToMain(role, finalUsername, email);
                    } else {
                        Log.d(TAG, "User " + email + " chưa có mật khẩu/chưa tạo. Chuyển đến CreatePasswordActivity.");
                        navigateToCreatePassword(email, accessToken, role, avatarUrl, finalUsername);
                    }
                });
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi kiểm tra mật khẩu user public: " + e.getMessage()
                        + ". Chuyển đến CreatePasswordActivity.");
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this,
                            "Lỗi kiểm tra thông tin user. Vui lòng tạo mật khẩu.", Toast.LENGTH_SHORT).show();
                    navigateToCreatePassword(email, accessToken, role, avatarUrl, username);
                });
            }
        });
    }

    private void fetchUsernameAndNavigate(final String email) {
        userService.getUserByEmail(email, new SupabaseCallback<User>() {
            @Override public void onSuccess(User user) {
                runOnUiThread(() -> {
                    String usernameToNavigate = "Unknown";
                    if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                        usernameToNavigate = user.getUsername();
                    } else if (email.contains("@")) {
                        usernameToNavigate = email.split("@")[0];
                        Log.w(TAG, "Không tìm thấy username cho " + email + ", dùng tạm phần trước @.");
                    }
                    String role = determineRole(email);
                    navigateToMain(role, usernameToNavigate, email);
                });
            }
            @Override public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lấy username cho " + email + ": " + e.getMessage()
                        + ". Dùng tạm phần trước @.");
                runOnUiThread(() -> {
                    String usernameToNavigate = email.contains("@") ? email.split("@")[0] : "Unknown";
                    String role = determineRole(email);
                    navigateToMain(role, usernameToNavigate, email);
                });
            }
        });
    }

    private String determineRole(String email) {
        return (email != null && email.endsWith("@uef.edu.vn")) ? "ADMIN" : "USER";
    }

    private void navigateToMain(String role, String username, String email) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("USER_USERNAME", username);
        intent.putExtra("USER_EMAIL", email);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToCreatePassword(String email,
                                          String accessToken,
                                          String role,
                                          String avatarUrl,
                                          String username) {
        Intent intent = new Intent(LoginActivity.this, CreatePasswordActivity.class);
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("ACCESS_TOKEN", accessToken);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("AVATAR_URL", avatarUrl);
        intent.putExtra("USER_USERNAME", username);
        startActivity(intent);
    }
}
