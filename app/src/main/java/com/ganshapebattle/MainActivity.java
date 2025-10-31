// File: com/ganshapebattle/MainActivity.java

package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // <<< Import Log
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton; // <<< Import ImageButton
import android.widget.TextView;    // <<< Import TextView
import android.widget.Toast; // <<< Import Toast

import androidx.activity.result.ActivityResultLauncher; // <<< Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // <<< Import ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.admin.AddEditGalleryActivity;
import com.ganshapebattle.admin.AddEditLobbyActivity;
import com.ganshapebattle.admin.AddEditPictureActivity;
import com.ganshapebattle.admin.AddEditPlayerActivity;
import com.ganshapebattle.admin.Admin;
import com.ganshapebattle.admin.CreateCategory;
import com.ganshapebattle.admin.GalleryCRUDActivity; //
import com.ganshapebattle.admin.GalleryDetailActivity;
import com.ganshapebattle.admin.LobbyCRUDActivity;
import com.ganshapebattle.admin.LobbyDetailActivity;
import com.ganshapebattle.admin.MenuActivity; //
import com.ganshapebattle.admin.PictureCRUDActivity;
import com.ganshapebattle.admin.PictureDetailActivity;
import com.ganshapebattle.admin.PlayerCRUDActivity;
import com.ganshapebattle.admin.PlayerDetailActivity;
import com.ganshapebattle.admin.UserCRUDActivity;
import com.ganshapebattle.admin.UserDetailActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// <<< Thêm import SessionManager >>>
import java.util.HashMap;
// <<< >>>

public class MainActivity extends AppCompatActivity {

    // Khai báo các view components
    private Button btnJoinGame, btnCreateLobby, btnGoToLeaderboard, btnGoToGallery, btnAdminPanel;

    private FloatingActionButton btnLogout;
    private ImageButton btnProfile; // Nút mở Profile
    private TextView tvCurrentUsername; // TextView hiển thị username
    private String currentUsername; // Biến lưu username hiện tại
    private String currentUserEmail; // Biến lưu email (vẫn cần để mở Profile)
    private String userRole; // Lưu vai trò người dùng

    // Launcher để khởi chạy ProfileActivity và nhận kết quả trả về
    private ActivityResultLauncher<Intent> profileActivityResultLauncher;

    // <<< Thêm SessionManager >>>
    private SessionManager sessionManager;
    // <<< >>>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //

        // <<< Khởi tạo SessionManager >>>
        sessionManager = new SessionManager(getApplicationContext());
        // <<< >>>

        // --- Ánh xạ View ---
        btnJoinGame = findViewById(R.id.btnJoinGame); //
        btnCreateLobby = findViewById(R.id.btnCreateLobby); //
        btnGoToLeaderboard = findViewById(R.id.btnGoToLeaderboard); //
        btnGoToGallery = findViewById(R.id.btnGoToGallery); //
        btnAdminPanel = findViewById(R.id.btnAdminPanel); //
        btnLogout = findViewById(R.id.btnLogout); //
        btnProfile = findViewById(R.id.btnProfile); //
        tvCurrentUsername = findViewById(R.id.tvCurrentUsername); //
        // --- ---

        // === LỖI ĐÃ ĐƯỢC XÓA ===
        // Intent intent1 = new Intent(MainActivity.this, VerifySignupOtpActivity.class);
        // startActivity(intent1);
        // =======================

        // <<< Lấy thông tin người dùng từ Session thay vì Intent >>>
        if (sessionManager.isLoggedIn()) {
            HashMap<String, String> userDetails = sessionManager.getUserDetails();
            userRole = userDetails.get(SessionManager.KEY_ROLE);
            currentUsername = userDetails.get(SessionManager.KEY_USERNAME);
            currentUserEmail = userDetails.get(SessionManager.KEY_EMAIL);
            Log.d("MainActivity", "User details loaded from session: Email=" + currentUserEmail + ", Username=" + currentUsername + ", Role=" + userRole);
        } else {
            // Trường hợp không có session (nên chuyển về Login, nhưng phòng ngừa)
            Log.e("MainActivity", "User is not logged in! Redirecting to LoginActivity.");
            sessionManager.logoutUser(); // Gọi logout để đảm bảo chuyển về Login
            finish(); // Đóng MainActivity
            return;
        }
        // <<< >>>

        // Hiển thị username ban đầu lên TextView
        updateUsernameDisplay();

        // Hiển thị nút "Bảng điều khiển Admin" nếu user có vai trò ADMIN
        if ("ADMIN".equals(userRole)) {
            btnAdminPanel.setVisibility(View.VISIBLE);
        } else {
            btnAdminPanel.setVisibility(View.GONE);
        }

        // --- Gắn sự kiện điều hướng cho các nút chức năng ---
        btnJoinGame.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LobbyUserActivity.class).putExtra("username",currentUsername))); //
        btnCreateLobby.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LobbyActivity.class).putExtra("username",currentUsername))); //
        btnGoToLeaderboard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LeaderHistoryActivity.class)));
        // Chuyển đến màn hình quản lý Gallery của admin
        btnGoToGallery.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GalleryCRUDActivity.class))); //
        btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MenuActivity.class))); //

        // <<< Sự kiện nút Đăng xuất: Gọi SessionManager >>>
        btnLogout.setOnClickListener(v -> {
            Log.d("MainActivity", "Logout button clicked.");
            sessionManager.logoutUser(); // Xóa session và chuyển về LoginActivity
            finish(); // Đóng MainActivity hiện tại
        });
        // <<< >>>
        // --- ---

        // === KHỞI TẠO ActivityResultLauncher ===
        // Đăng ký launcher để nhận kết quả trả về từ ProfileActivity
        profileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Kiểm tra xem ProfileActivity có trả về kết quả OK không
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Intent data = result.getData();
                        // Kiểm tra xem Intent trả về có chứa username đã cập nhật không
                        if (data != null && data.hasExtra("UPDATED_USERNAME")) {
                            String updatedUsername = data.getStringExtra("UPDATED_USERNAME");
                            if (updatedUsername != null && !updatedUsername.isEmpty() && !updatedUsername.equals(currentUsername)) {
                                Log.d("MainActivity", "Nhận được username cập nhật từ ProfileActivity: " + updatedUsername);
                                currentUsername = updatedUsername; // Cập nhật biến username hiện tại

                                // <<< Cập nhật lại session với username mới >>>
                                sessionManager.createLoginSession(currentUserEmail, currentUsername, userRole);
                                Log.d("MainActivity", "Session updated with new username.");
                                // <<< >>>

                                updateUsernameDisplay(); // Cập nhật lại TextView hiển thị username
                            } else {
                                Log.d("MainActivity", "ProfileActivity trả về OK nhưng username không thay đổi hoặc bị thiếu.");
                            }
                        } else {
                            Log.d("MainActivity", "ProfileActivity trả về OK nhưng không có dữ liệu username cập nhật.");
                            // Có thể thêm logic tải lại username từ server nếu cần thiết, nhưng nên dựa vào session đã cập nhật
                        }
                    } else {
                        // Ghi log nếu ProfileActivity không trả về OK (ví dụ: người dùng nhấn Back)
                        Log.d("MainActivity", "ProfileActivity không trả về RESULT_OK (Code: " + result.getResultCode() + ")");
                    }
                });
        // =====================================


        // <<< Sự kiện cho nút Profile >>>
        btnProfile.setOnClickListener(v -> {
            // Kiểm tra xem email có hợp lệ không trước khi mở ProfileActivity
            if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("USER_EMAIL", currentUserEmail); // <<< Vẫn truyền email sang ProfileActivity
                // <<< Sử dụng launcher đã đăng ký để khởi chạy ProfileActivity >>>
                profileActivityResultLauncher.launch(intent);
                // <<< >>>
            } else {
                // Xử lý trường hợp email bị thiếu (ít xảy ra nếu đã đăng nhập)
                Log.e("MainActivity", "currentUserEmail là null hoặc rỗng, không thể mở ProfileActivity.");
                Toast.makeText(MainActivity.this, "Lỗi: Không thể mở hồ sơ (thiếu email).", Toast.LENGTH_SHORT).show();
            }
        });
        // <<< >>>
    }

    /**
     * Hàm tiện ích để cập nhật TextView hiển thị username.
     * Sử dụng giá trị mặc định nếu username bị null hoặc rỗng.
     */
    private void updateUsernameDisplay() {
        if (currentUsername != null && !currentUsername.isEmpty()) {
            tvCurrentUsername.setText(currentUsername); // Hiển thị username
            tvCurrentUsername.setVisibility(View.VISIBLE);
        } else {
            tvCurrentUsername.setText("Người dùng"); // Hiển thị giá trị mặc định
            tvCurrentUsername.setVisibility(View.VISIBLE);
            Log.w("MainActivity", "currentUsername là null hoặc rỗng khi cập nhật hiển thị.");
        }
    }
}