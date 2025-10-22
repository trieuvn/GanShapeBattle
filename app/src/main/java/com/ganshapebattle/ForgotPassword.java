// File: com/ganshapebattle/ForgotPassword.java

package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

public class ForgotPassword extends AppCompatActivity {

    private EditText inputEmail;
    private Button btnSendReset;
    private TextView backToLogin;
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        userService = new UserService();
        inputEmail = findViewById(R.id.inputEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        backToLogin = findViewById(R.id.backToLogin);

        // Đổi tên hàm gọi ở đây
        btnSendReset.setOnClickListener(v -> performPasswordResetRequest());
        backToLogin.setOnClickListener(v -> finish());
    }

    private void performPasswordResetRequest() {
        String email = inputEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        // SỬA Ở ĐÂY: Gọi lại hàm sendPasswordReset, KHÔNG phải sendPasswordResetOtp
        userService.sendPasswordReset(email, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(ForgotPassword.this, "Một mã OTP đã được gửi đến email của bạn.", Toast.LENGTH_LONG).show();

                    // Vẫn chuyển sang màn hình xác thực OTP như cũ
                    Intent intent = new Intent(ForgotPassword.this, VerifyOtpActivity.class);
                    intent.putExtra("USER_EMAIL", email);
                    startActivity(intent);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ForgotPassword.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}