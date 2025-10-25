// File: com/ganshapebattle/LoginActivity.java

package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private TextView gotoRegister, forgotPassword;
    private UserService userService;
    private ImageView googleLogin;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private static final String TAG = "AuthSignIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); //

        userService = new UserService(); //
        inputEmail = findViewById(R.id.inputEmail); //
        inputPassword = findViewById(R.id.inputPassword); //
        btnLogin = findViewById(R.id.btnLogin); //
        gotoRegister = findViewById(R.id.gotoRegister); //
        forgotPassword = findViewById(R.id.forgotPassword); //
        googleLogin = findViewById(R.id.googleLogin); //

        String webClientId = "772500808523-en9rpcf5rm76irm7sf5cul4ue2ogpnk2.apps.googleusercontent.com";
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account == null || account.getIdToken() == null) {
                                Log.w(TAG, "Google sign in failed: Account hoặc ID Token null");
                                Toast.makeText(this, "Không thể lấy ID Token từ Google.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Log.d(TAG, "Got Google ID Token. Proceeding with Supabase.");
                            supabaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            Toast.makeText(this, "Đăng nhập Google thất bại.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnLogin.setOnClickListener(v -> performLogin());
        gotoRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, Register.class))); //
        forgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPassword.class))); //
        googleLogin.setOnClickListener(v -> signInWithGoogle());
    }

    private void performLogin() {
        // <<< Lấy email từ input field MỘT LẦN ở đây >>>
        final String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.loginUser(email, password, new SupabaseCallback<String>() { //
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                    // <<< Truyền email đã lấy ban nãy vào fetchUsernameAndNavigate >>>
                    fetchUsernameAndNavigate(email);
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Login failed", e);
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void supabaseAuthWithGoogle(String idToken) {
        userService.signInWithGoogle(idToken, new SupabaseCallback<Map<String, Object>>() { //
            @Override
            public void onSuccess(Map<String, Object> sessionMap) {
                runOnUiThread(() -> {
                    try {
                        Map<String, Object> userMap = (Map<String, Object>) sessionMap.get("user");
                        // <<< Lấy email MỘT LẦN ở đây >>>
                        final String email = (String) userMap.get("email");
                        String accessToken = (String) sessionMap.get("access_token");
                        Map<String, Object> meta = (Map<String, Object>) userMap.get("user_metadata");
                        String avatarUrl = (meta != null) ? (String) meta.get("avatar_url") : null;
                        String displayName = (meta != null) ? (String) meta.get("full_name") : null;
                        String usernameForCheck = (displayName != null && !displayName.isEmpty()) ? displayName : (email != null && email.contains("@") ? email.split("@")[0] : "User");

                        if (email == null || accessToken == null) {
                            Log.e(TAG, "Email hoặc Access Token null sau khi đăng nhập Google Supabase.");
                            Toast.makeText(LoginActivity.this, "Lỗi: Không lấy được thông tin user.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công với " + email, Toast.LENGTH_SHORT).show();
                        String role = determineRole(email);

                        Log.d(TAG, "Kiểm tra sự tồn tại mật khẩu cho: " + email);
                        // <<< Truyền email đã lấy ở trên vào checkIfPasswordExists >>>
                        checkIfPasswordExists(email, accessToken, role, avatarUrl, usernameForCheck);

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi phân tích dữ liệu Supabase sau khi đăng nhập Google", e);
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công nhưng lỗi xử lý dữ liệu.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Supabase Google Auth Failed", e);
                    Toast.makeText(LoginActivity.this, "Xác thực Supabase thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Kiểm tra mật khẩu và điều hướng, truyền username VÀ email.
     */
    private void checkIfPasswordExists(final String email, String accessToken, String role, String avatarUrl, String username) { // <<< Thêm final cho email
        userService.getUserByEmail(email, new SupabaseCallback<User>() { //
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    String finalUsername = (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) ? user.getUsername() : username;
                    if (user != null && user.getPassword() != null && !user.getPassword().isEmpty()) {
                        Log.d(TAG, "User " + email + " đã có mật khẩu. Chuyển đến MainActivity.");
                        // <<< Truyền cả role, finalUsername, và email gốc >>>
                        navigateToMain(role, finalUsername, email);
                    } else {
                        Log.d(TAG, "User " + email + " chưa có mật khẩu/chưa tạo. Chuyển đến CreatePasswordActivity.");
                        // <<< Truyền cả email, accessToken, role, avatarUrl, finalUsername >>>
                        navigateToCreatePassword(email, accessToken, role, avatarUrl, finalUsername);
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi kiểm tra mật khẩu user public: " + e.getMessage() + ". Chuyển đến CreatePasswordActivity.");
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Lỗi kiểm tra thông tin user. Vui lòng tạo mật khẩu.", Toast.LENGTH_SHORT).show();
                    // <<< Truyền cả email, accessToken, role, avatarUrl, username dự phòng >>>
                    navigateToCreatePassword(email, accessToken, role, avatarUrl, username);
                });
            }
        });
    }

    /**
     * Lấy username từ email rồi mới điều hướng (cho luồng đăng nhập email/pass).
     */
    private void fetchUsernameAndNavigate(final String email) { // <<< Thêm final cho email
        userService.getUserByEmail(email, new SupabaseCallback<User>() { //
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    String usernameToNavigate = "Unknown";
                    if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                        usernameToNavigate = user.getUsername();
                    } else if (email.contains("@")) {
                        usernameToNavigate = email.split("@")[0];
                        Log.w(TAG, "Không tìm thấy username cho " + email + ", dùng tạm phần trước @.");
                    }
                    String role = determineRole(email);
                    // <<< Truyền cả role, usernameToNavigate, và email gốc >>>
                    navigateToMain(role, usernameToNavigate, email);
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lấy username cho " + email + ": " + e.getMessage() + ". Dùng tạm phần trước @.");
                runOnUiThread(() -> {
                    String usernameToNavigate = email.contains("@") ? email.split("@")[0] : "Unknown";
                    String role = determineRole(email);
                    // <<< Truyền cả role, usernameToNavigate, và email gốc >>>
                    navigateToMain(role, usernameToNavigate, email);
                });
            }
        });
    }

    private String determineRole(String email) {
        if (email != null && email.endsWith("@uef.edu.vn")) {
            return "ADMIN";
        }
        return "USER";
    }

    /**
     * CẬP NHẬT: Chuyển hướng đến MainActivity, gửi USER_USERNAME và USER_EMAIL.
     * @param email Email người dùng (cần cho ProfileActivity).
     */
    private void navigateToMain(String role, String username, String email) { // <<< Thêm tham số email
        Intent intent = new Intent(LoginActivity.this, MainActivity.class); //
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("USER_USERNAME", username); // <<< Gửi username
        intent.putExtra("USER_EMAIL", email); // <<< Gửi email
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Chuyển hướng đến CreatePasswordActivity, gửi USERNAME thay vì DISPLAY_NAME.
     */
    private void navigateToCreatePassword(String email, String accessToken, String role, String avatarUrl, String username) {
        Intent intent = new Intent(LoginActivity.this, CreatePasswordActivity.class); //
        intent.putExtra("USER_EMAIL", email);
        intent.putExtra("ACCESS_TOKEN", accessToken);
        intent.putExtra("USER_ROLE", role);
        intent.putExtra("AVATAR_URL", avatarUrl);
        intent.putExtra("USER_USERNAME", username); // <<< Truyền username
        startActivity(intent);
    }
}