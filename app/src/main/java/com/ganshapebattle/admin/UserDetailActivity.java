package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ganshapebattle.R;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

public class UserDetailActivity extends AppCompatActivity {

    private static final String TAG = "UserDetailActivity";

    private ImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvPhoneNumber, tvDateOfBirth;
    private Button btnUpdateUser, btnDeleteUser;

    private UserService userService;
    private String currentUsername;

    // Launcher để nhận kết quả từ AddEditUserActivity
    private ActivityResultLauncher<Intent> editUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth);
        btnUpdateUser = findViewById(R.id.btnUpdateUser);
        btnDeleteUser = findViewById(R.id.btnDeleteUser);

        userService = new UserService();

        currentUsername = getIntent().getStringExtra("USER_USERNAME");

        if (currentUsername != null && !currentUsername.isEmpty()) {
            fetchUserDetails(currentUsername);
        } else {
            Toast.makeText(this, "Không tìm thấy username", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Mở AddEditUserActivity để chỉnh sửa người dùng
        btnUpdateUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailActivity.this, AddEditUserActivity.class);
            intent.putExtra("USER_USERNAME_EDIT", currentUsername); // Truyền username để báo hiệu chế độ chỉnh sửa
            editUserLauncher.launch(intent);
        });

        btnDeleteUser.setOnClickListener(v -> {
            deleteUser(currentUsername);
        });

        // Khởi tạo ActivityResultLauncher
        editUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Nếu AddEditUserActivity trả về RESULT_OK, tức là có thay đổi
                        // (sửa thành công), thì tải lại thông tin chi tiết user
                        fetchUserDetails(currentUsername);
                    }
                }
        );
    }

    // Phương thức onResume không còn cần thiết vì đã dùng ActivityResultLauncher
    // @Override
    // protected void onResume() {
    //     super.onResume();
    //     if (currentUsername != null && !currentUsername.isEmpty()) {
    //         fetchUserDetails(currentUsername);
    //     }
    // }

    private void fetchUserDetails(String username) {
        // ... (Giữ nguyên logic fetchUserDetails như cũ) ...
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        tvUsername.setText(user.getUsername());
                        tvEmail.setText(user.getEmail());
                        tvPhoneNumber.setText(user.getPhoneNumber());
                        tvDateOfBirth.setText(user.getDateOfBirth());

                        Glide.with(UserDetailActivity.this)
                                .load(user.getAvatar())
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .into(ivAvatar);
                    } else {
                        Toast.makeText(UserDetailActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
                        // Có thể đóng activity hoặc reset UI nếu user không còn tồn tại
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lấy thông tin user: ", e);
                runOnUiThread(() -> Toast.makeText(UserDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void deleteUser(String username) {
        // ... (Giữ nguyên logic deleteUser như cũ) ...
        userService.deleteUser(username, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(UserDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    // Đánh dấu RESULT_OK để UserCRUDActivity biết cần load lại danh sách
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi xóa user: ", e);
                runOnUiThread(() -> Toast.makeText(UserDetailActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}