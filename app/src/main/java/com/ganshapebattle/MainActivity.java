// File: com/ganshapebattle/MainActivity.java

package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.admin.MenuActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnGoToDrawing, btnGoToGameRoom, btnGoToLeaderboard, btnGoToGallery, btnAdminPanel, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ tất cả các nút
        btnGoToDrawing = findViewById(R.id.btnGoToDrawing);
        btnGoToGameRoom = findViewById(R.id.btnGoToGameRoom);
        btnGoToLeaderboard = findViewById(R.id.btnGoToLeaderboard);
        btnGoToGallery = findViewById(R.id.btnGoToGallery);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
        btnLogout = findViewById(R.id.btnLogout);

        // Lấy vai trò người dùng và hiển thị nút Admin nếu cần
        String userRole = getIntent().getStringExtra("USER_ROLE");
        if ("ADMIN".equals(userRole)) {
            btnAdminPanel.setVisibility(View.VISIBLE);
        } else {
            btnAdminPanel.setVisibility(View.GONE);
        }

        // --- Gắn sự kiện điều hướng cho các nút ---

        btnGoToDrawing.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DesignActivity.class));
        });

        btnGoToGameRoom.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, GameRoom.class));
        });

        btnGoToLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Leaderboard.class));
        });

        btnGoToGallery.setOnClickListener(v -> {
            // Lưu ý: Tên class cho Gallery của bạn là Gallery.java trong package com.ganshapebattle.admin
            // Nếu bạn có một màn hình Gallery khác cho người dùng, hãy thay đổi tên class ở đây.
            startActivity(new Intent(MainActivity.this, com.ganshapebattle.admin.Gallery.class));
        });

        btnAdminPanel.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MenuActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}