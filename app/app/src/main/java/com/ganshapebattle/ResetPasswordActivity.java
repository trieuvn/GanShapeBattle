// File: com/ganshapebattle/ResetPasswordActivity.java

package com.ganshapebattle;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.HashMap;
import java.util.Map;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText inputNewPassword, inputConfirmNewPassword;
    private Button btnUpdatePassword;
    private UserService userService;
    private String accessToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        userService = new UserService();
        inputNewPassword = findViewById(R.id.inputNewPassword);
        inputConfirmNewPassword = findViewById(R.id.inputConfirmNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        // Lấy dữ liệu từ deep link
        Uri data = getIntent().getData();
        if (data != null && data.getFragment() != null) {
            // Token nằm trong phần "fragment" của URL (sau dấu #)
            // Ví dụ: ...#access_token=...&refresh_token=...
            Map<String, String> params = parseFragment(data.getFragment());
            accessToken = params.get("access_token");
        }

        if (accessToken == null) {
            Toast.makeText(this, "Liên kết không hợp lệ hoặc đã hết hạn", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    // Hàm tiện ích để phân tích chuỗi fragment
    private Map<String, String> parseFragment(String fragment) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = fragment.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            params.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return params;
    }

    private void updatePassword() {
        String newPassword = inputNewPassword.getText().toString();
        String confirmPassword = inputConfirmNewPassword.getText().toString();

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.updateUserPassword(accessToken, newPassword, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(ResetPasswordActivity.this, result, Toast.LENGTH_LONG).show();
                    finish(); // Đóng màn hình reset và quay lại màn hình đăng nhập
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ResetPasswordActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}