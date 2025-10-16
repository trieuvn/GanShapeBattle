package com.ganshapebattle;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ganshapebattle.service.SupabaseService;

public class MainActivity extends AppCompatActivity {

    private TextView hello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Ánh xạ TextView từ layout
        hello = findViewById(R.id.hello);

        // Khởi tạo SupabaseService
        SupabaseService client = new SupabaseService();

        // Gọi phương thức getUsers và xử lý kết quả trả về
        client.getUsers(new SupabaseService.SupabaseCallback() {
            @Override
            public void onSuccess(String result) {
                // Cập nhật giao diện trên luồng chính (UI Thread)
                runOnUiThread(() -> {
                    hello.setText("Thành công! Dữ liệu nhận được:\n" + result);
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Cập nhật giao diện trên luồng chính (UI Thread)
                runOnUiThread(() -> {
                    hello.setText("Đã xảy ra lỗi:\n" + e.getMessage());
                });
            }
        });
    }
}