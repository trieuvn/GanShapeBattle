// File: com/ganshapebattle/VerifySignupOtpActivity.java

package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

public class VerifySignupOtpActivity extends AppCompatActivity {

    private EditText inputSignupOtp;
    private Button btnVerifySignup;
    private UserService userService;
    private String userEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_signup_otp);

        userService = new UserService();
        inputSignupOtp = findViewById(R.id.inputSignupOtp);
        btnVerifySignup = findViewById(R.id.btnVerifySignup);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null) {
            Toast.makeText(this, "Có lỗi xảy ra, không tìm thấy email.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnVerifySignup.setOnClickListener(v -> performOtpVerification());
    }

    private void performOtpVerification() {
        String otp = inputSignupOtp.getText().toString().trim();

        if (TextUtils.isEmpty(otp) || otp.length() < 6) {
            Toast.makeText(this, "Vui lòng nhập mã OTP gồm 6 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.verifySignupOtp(userEmail, otp, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(VerifySignupOtpActivity.this, result, Toast.LENGTH_LONG).show();
                    // Kích hoạt thành công, chuyển đến màn hình đăng nhập
                    Intent intent = new Intent(VerifySignupOtpActivity.this, LoginActivity.class);
                    // Xóa các màn hình trước đó để người dùng không quay lại được
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(VerifySignupOtpActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}