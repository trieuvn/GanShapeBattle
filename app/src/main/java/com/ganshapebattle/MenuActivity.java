package com.ganshapebattle; // Thay đổi thành package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Ánh xạ các nút từ layout
        Button btnManageUsers = findViewById(R.id.btnManageUsers);
        Button btnManageLobbies = findViewById(R.id.btnManageLobbies);
        Button btnManageGalleries = findViewById(R.id.btnManageGalleries);
        Button btnManagePictures = findViewById(R.id.btnManagePictures);
        Button btnManagePlayers = findViewById(R.id.btnManagePlayers);

        // --- Thiết lập sự kiện click cho các nút ---

        // Chuyển đến màn hình quản lý User
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, UserCRUDActivity.class);
            startActivity(intent);
        });

        // (Các chức năng khác sẽ được thêm sau)
        btnManageLobbies.setOnClickListener(v -> {
             Intent intent = new Intent(MenuActivity.this, LobbyCRUDActivity.class);
             startActivity(intent);
        });

        btnManageGalleries.setOnClickListener(v -> {
             Intent intent = new Intent(MenuActivity.this, GalleryCRUDActivity.class);
             startActivity(intent);
        });

        btnManagePictures.setOnClickListener(v -> {
             Intent intent = new Intent(MenuActivity.this, PictureCRUDActivity.class);
             startActivity(intent);
        });

        btnManagePlayers.setOnClickListener(v -> {
             Intent intent = new Intent(MenuActivity.this, PlayerCRUDActivity.class);
             startActivity(intent);
        });
    }
}