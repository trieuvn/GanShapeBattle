package com.ganshapebattle.admin; // Hoặc package com.ganshapebattle tùy thuộc vào vị trí tệp

import android.content.Intent;
import android.graphics.Bitmap; // Import Bitmap
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
// Import ImageUtils
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

// Đảm bảo bạn đã import CircleImageView nếu dùng
// import de.hdodenhof.circleimageview.CircleImageView;

public class UserDetailActivity extends AppCompatActivity {

    private static final String TAG = "UserDetailActivity";

    // Thay ImageView bằng CircleImageView nếu layout của bạn dùng nó
    private ImageView ivAvatar;
    // private CircleImageView ivAvatar;
    private TextView tvUsername, tvEmail, tvPhoneNumber, tvDateOfBirth;
    private Button btnUpdateUser, btnDeleteUser;

    private UserService userService;
    private String currentUsername;

    private ActivityResultLauncher<Intent> editUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        // Đảm bảo ID ánh xạ đúng với layout của bạn
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

        btnUpdateUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserDetailActivity.this, AddEditUserActivity.class);
            intent.putExtra("USER_USERNAME_EDIT", currentUsername);
            editUserLauncher.launch(intent);
        });

        btnDeleteUser.setOnClickListener(v -> {
            // Thêm hộp thoại xác nhận trước khi xóa (nên làm)
            deleteUser(currentUsername);
        });

        editUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchUserDetails(currentUsername); // Tải lại dữ liệu nếu có thay đổi
                    }
                }
        );
    }

    private void fetchUserDetails(String username) {
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        tvUsername.setText(user.getUsername());
                        tvEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                        tvPhoneNumber.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
                        tvDateOfBirth.setText(user.getDateOfBirth() != null ? user.getDateOfBirth() : "N/A");

                        // --- CẬP NHẬT LOGIC HIỂN THỊ AVATAR ---
                        String avatarData = user.getAvatar();
                        if (avatarData != null && !avatarData.isEmpty()) {
                            if (avatarData.startsWith("http")) {
                                // Dữ liệu cũ là URL, dùng Glide
                                Glide.with(UserDetailActivity.this)
                                        .load(avatarData)
                                        .placeholder(R.drawable.ic_default_avatar) // Ảnh chờ tải
                                        .error(R.drawable.ic_default_avatar) // Ảnh khi lỗi
                                        .into(ivAvatar);
                            } else {
                                // Dữ liệu mới là Base64, giải mã
                                Bitmap avatarBitmap = ImageUtils.base64ToBitmap(avatarData);
                                if (avatarBitmap != null) {
                                    ivAvatar.setImageBitmap(avatarBitmap); // Hiển thị Bitmap
                                } else {
                                    // Lỗi giải mã Base64
                                    Log.w(TAG, "Lỗi giải mã Base64 avatar cho user: " + username);
                                    ivAvatar.setImageResource(R.drawable.ic_default_avatar); // Ảnh mặc định
                                }
                            }
                        } else {
                            // Không có avatar
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar); // Ảnh mặc định
                        }
                        // --- KẾT THÚC CẬP NHẬT ---

                    } else {
                        Toast.makeText(UserDetailActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_LONG).show();
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
        userService.deleteUser(username, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(UserDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo cho Activity trước đó biết là đã xóa thành công
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