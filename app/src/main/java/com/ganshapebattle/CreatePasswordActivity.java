package com.ganshapebattle; // Hoặc package của bạn

import android.content.Intent;
import android.graphics.Bitmap; // <<< Import Bitmap
import android.graphics.drawable.Drawable; // <<< Import Drawable
import android.os.Bundle;
import android.os.Handler; // <<< Import Handler
import android.os.Looper; // <<< Import Looper
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull; // <<< Import NonNull
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// <<< Import Glide và các lớp liên quan >>>
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
// <<< >>>

import com.ganshapebattle.models.User; // <<< Import User model
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;
import com.ganshapebattle.utils.ImageUtils; // <<< Import ImageUtils

import java.util.concurrent.ExecutorService; // <<< Import ExecutorService
import java.util.concurrent.Executors; // <<< Import Executors


public class CreatePasswordActivity extends AppCompatActivity {

    private static final String TAG = "CreatePasswordActivity";

    private EditText inputNewPassword, inputConfirmNewPassword;
    private Button btnSavePassword;
    private UserService userService;
    private String userEmail;
    private String accessToken;
    private String userRole;
    private String avatarUrl; // <<< Lưu avatar URL từ Intent
    private String displayName; // <<< Lưu displayName từ Intent
    private boolean isSaving = false; // <<< Cờ ngăn nhấn nút Lưu nhiều lần

    // <<< Executor để xử lý Base64 trên background thread >>>
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    // <<< >>>

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password); //

        userService = new UserService(); //
        inputNewPassword = findViewById(R.id.inputNewPassword); //
        inputConfirmNewPassword = findViewById(R.id.inputConfirmNewPassword); //
        btnSavePassword = findViewById(R.id.btnSavePassword); //

        // Nhận dữ liệu từ LoginActivity
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        accessToken = getIntent().getStringExtra("ACCESS_TOKEN");
        userRole = getIntent().getStringExtra("USER_ROLE");
        avatarUrl = getIntent().getStringExtra("AVATAR_URL"); // <<< Lấy avatar URL
        displayName = getIntent().getStringExtra("DISPLAY_NAME"); // <<< Lấy displayName

        // === KIỂM TRA NULL VÀ TẠO GIÁ TRỊ MẶC ĐỊNH ===
        // Chỉ kiểm tra các giá trị bắt buộc cốt lõi cho Auth update
        if (userEmail == null || accessToken == null || userRole == null) {
            Log.e(TAG, "Thiếu thông tin userEmail, accessToken hoặc userRole.");
            Toast.makeText(this, "Lỗi: Thiếu thông tin người dùng cốt lõi.", Toast.LENGTH_LONG).show();
            finish(); // Đóng activity nếu thiếu thông tin cơ bản
            return;
        }

        // Nếu displayName bị thiếu, tạo giá trị mặc định từ email
        if (displayName == null || displayName.isEmpty()) {
            Log.w(TAG, "displayName bị thiếu từ Intent, tạo giá trị mặc định từ email.");
            if (userEmail.contains("@")) {
                displayName = userEmail.split("@")[0];
            } else {
                displayName = "Người dùng mới"; // Giá trị dự phòng cuối cùng
            }
            Log.d(TAG, "displayName mặc định được gán: " + displayName);
        }
        // ============================================

        btnSavePassword.setOnClickListener(v -> {
            if (!isSaving) { // <<< Kiểm tra cờ isSaving
                savePasswordAndInsertUser();
            }
        });
    }

    /**
     * Lưu mật khẩu vào Supabase Auth, sau đó tạo (Insert) user vào bảng public User.
     */
    private void savePasswordAndInsertUser() {
        isSaving = true; // <<< Đặt cờ isSaving
        btnSavePassword.setEnabled(false); // <<< Vô hiệu hóa nút Lưu
        btnSavePassword.setText("Đang xử lý..."); // <<< Thay đổi text nút

        String newPassword = inputNewPassword.getText().toString();
        String confirmPassword = inputConfirmNewPassword.getText().toString();

        // --- Validate Input ---
        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            showErrorAndResetButton("Vui lòng nhập đầy đủ mật khẩu");
            return;
        }
        if (newPassword.length() < 6) {
            showErrorAndResetButton("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showErrorAndResetButton("Mật khẩu xác nhận không khớp");
            return;
        }

        // Bước 1: Cập nhật mật khẩu trong Supabase Auth
        userService.updateUserPassword(accessToken, newPassword, new SupabaseCallback<String>() { //
            @Override
            public void onSuccess(String authUpdateResult) {
                Log.d(TAG, "Cập nhật mật khẩu Auth thành công: " + authUpdateResult);

                // Bước 2: Chuẩn bị tạo User object và xử lý Avatar Base64
                User newUser = new User(); //
                newUser.setEmail(userEmail);
                newUser.setPassword(newPassword); // <<< Lưu mật khẩu mới (plain text)
                newUser.setUsername(displayName); // <<< Sử dụng displayName (đã có giá trị mặc định nếu cần)
                // Các trường khác để null
                newUser.setPhoneNumber(null);
                newUser.setDateOfBirth(null);

                // Xử lý Avatar URL -> Base64
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Log.d(TAG, "Bắt đầu tải và chuyển đổi avatar: " + avatarUrl);
                    Glide.with(CreatePasswordActivity.this)
                            .asBitmap()
                            .load(avatarUrl)
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    Log.d(TAG, "Tải avatar thành công, bắt đầu chuyển đổi Base64...");
                                    executor.execute(() -> {
                                        // Sử dụng PNG để giữ chất lượng, JPEG nếu muốn file nhỏ hơn
                                        String base64Avatar = ImageUtils.bitmapToBase64(resource, Bitmap.CompressFormat.PNG, 100); //
                                        handler.post(() -> { // Quay lại main thread
                                            if (base64Avatar != null) {
                                                Log.d(TAG, "Chuyển đổi Base64 thành công.");
                                                newUser.setAvatar(base64Avatar); // <<< Set Base64 avatar
                                            } else {
                                                Log.e(TAG, "Chuyển đổi Base64 thất bại.");
                                                // Không set avatar nếu chuyển đổi lỗi
                                            }
                                            insertUserToPublicTable(newUser); // <<< Gọi hàm insert sau khi xử lý avatar
                                        });
                                    });
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    Log.e(TAG, "Tải avatar thất bại từ URL: " + avatarUrl);
                                    // Tải thất bại, vẫn tiếp tục insert user không có avatar
                                    newUser.setAvatar(null); // Đảm bảo avatar là null
                                    insertUserToPublicTable(newUser);
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) { }
                            });
                } else {
                    // Không có avatar URL, insert user ngay lập tức
                    Log.d(TAG, "Không có avatar URL, tiến hành insert user.");
                    newUser.setAvatar(null); // Đảm bảo avatar là null
                    insertUserToPublicTable(newUser);
                }
            }

            @Override
            public void onFailure(Exception authUpdateError) {
                Log.e(TAG, "Lỗi cập nhật mật khẩu Auth: ", authUpdateError);
                showErrorAndResetButton("Lỗi lưu mật khẩu (Auth): " + authUpdateError.getMessage());
            }
        });
    }

    /**
     * Gọi API để Insert user vào bảng public User.
     * @param userToInsert Đối tượng User chứa thông tin cần insert.
     */
    private void insertUserToPublicTable(User userToInsert) {
        Log.d(TAG, "Gọi insertUser cho: Email=" + userToInsert.getEmail() + ", Username=" + userToInsert.getUsername());
        userService.insertUser(userToInsert, new SupabaseCallback<String>() { //
            @Override
            public void onSuccess(String insertResult) {
                Log.d(TAG, "Insert user vào bảng public thành công: " + insertResult);
                runOnUiThread(() -> {
                    Toast.makeText(CreatePasswordActivity.this, "Tạo mật khẩu và tài khoản thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMain(); // Chuyển sang MainActivity
                });
            }

            @Override
            public void onFailure(Exception insertError) {
                Log.e(TAG, "Lỗi insert user vào bảng public: ", insertError);
                // Lỗi có thể do username trùng (nếu Google Display Name không duy nhất và cột username là UNIQUE)
                // Hoặc do RLS, lỗi mạng...
                String errorMessage = "Lỗi lưu thông tin user: " + insertError.getMessage();
                if (insertError.getMessage() != null && insertError.getMessage().contains("duplicate key value violates unique constraint")) {
                    errorMessage = "Tên hiển thị '" + userToInsert.getUsername() + "' đã được sử dụng. Vui lòng liên hệ quản trị viên hoặc thử lại.";
                }
                showErrorAndResetButton(errorMessage);
            }
        });
    }

    /**
     * Hiển thị Toast lỗi và kích hoạt lại nút Lưu.
     * @param message Nội dung lỗi cần hiển thị.
     */
    private void showErrorAndResetButton(String message) {
        runOnUiThread(() -> {
            Toast.makeText(CreatePasswordActivity.this, message, Toast.LENGTH_LONG).show();
            isSaving = false; // <<< Reset cờ isSaving
            btnSavePassword.setEnabled(true); // <<< Kích hoạt lại nút
            btnSavePassword.setText("Lưu mật khẩu và tiếp tục"); // <<< Đặt lại text nút
        });
    }

    /**
     * Chuyển hướng đến MainActivity sau khi hoàn tất.
     */
    private void navigateToMain() {
        Intent intent = new Intent(CreatePasswordActivity.this, MainActivity.class); //
        intent.putExtra("USER_ROLE", userRole); // Chuyển tiếp vai trò
        // Xóa các màn hình trước đó (Login) khỏi back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Đóng màn hình tạo mật khẩu
    }

    /**
     * Hủy executor khi Activity bị hủy để tránh rò rỉ.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow(); // Ngắt các tác vụ đang chạy nếu có
    }
}