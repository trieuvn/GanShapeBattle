// File: com/ganshapebattle/VerifyOtpActivity.java

package com.ganshapebattle;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText inputOtp, inputNewPassword, inputConfirmNewPassword;
    private Button btnVerifyAndReset;
    private UserService userService;
    private String userEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        userService = new UserService();
        inputOtp = findViewById(R.id.inputOtp);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        inputConfirmNewPassword = findViewById(R.id.inputConfirmNewPassword);
        btnVerifyAndReset = findViewById(R.id.btnVerifyAndReset);

        // Nhận email từ màn hình ForgotPassword
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null) {
            Toast.makeText(this, "Có lỗi xảy ra, không tìm thấy email.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnVerifyAndReset.setOnClickListener(v -> performVerificationAndReset());
    }

    // Trong tệp VerifyOtpActivity.java

    private void performVerificationAndReset() {
        String otp = inputOtp.getText().toString().trim();
        String newPassword = inputNewPassword.getText().toString();
        String confirmPassword = inputConfirmNewPassword.getText().toString();

        // === KIỂM TRA ĐẦU VÀO ===
        if (TextUtils.isEmpty(otp) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
            return;
        }
        // ================== THÊM KIỂM TRA MỚI ==================
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        // =========================================================

        // Bước 1: Xác thực OTP để lấy access token
        userService.verifyPasswordResetOtp(userEmail, otp, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String accessToken) {
                // Bước 2: Dùng access token để cập nhật mật khẩu mới
                userService.updateUserPassword(accessToken, newPassword, new SupabaseCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        runOnUiThread(() -> {
                            Toast.makeText(VerifyOtpActivity.this, result, Toast.LENGTH_LONG).show();
                            finish(); // Hoàn thành, đóng màn hình
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(VerifyOtpActivity.this, "Lỗi cập nhật mật khẩu: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(VerifyOtpActivity.this, "Lỗi xác thực OTP: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}