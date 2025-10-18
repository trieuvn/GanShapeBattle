package com.ganshapebattle; // Thay đổi thành package của bạn

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddEditUserActivity extends AppCompatActivity {

    private static final String TAG = "AddEditUserActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvTitle;
    private CircleImageView ivAvatar;
    private Button btnSelectAvatar, btnSaveUser;
    private EditText etUsername, etPassword, etEmail, etPhoneNumber, etDateOfBirth;

    private UserService userService;
    private String currentUsername = null; // null nếu là thêm mới, có giá trị nếu là chỉnh sửa
    private Uri selectedImageUri; // Lưu URI của ảnh đã chọn

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private User userToEdit; // <-- THÊM BIẾN NÀY

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_user);

        // Ánh xạ View
        tvTitle = findViewById(R.id.tvTitle);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnSelectAvatar = findViewById(R.id.btnSelectAvatar);
        btnSaveUser = findViewById(R.id.btnSaveUser);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etDateOfBirth = findViewById(R.id.etDateOfBirth);

        userService = new UserService();

        // Kiểm tra xem có đang ở chế độ chỉnh sửa hay không
        Intent intent = getIntent();
        if (intent.hasExtra("USER_USERNAME_EDIT")) {
            currentUsername = intent.getStringExtra("USER_USERNAME_EDIT");
            tvTitle.setText("Chỉnh sửa người dùng");
            etUsername.setEnabled(false); // Không cho phép chỉnh sửa username
            loadUserDetailsForEdit(currentUsername);
        } else {
            tvTitle.setText("Thêm người dùng mới");
        }

        // --- Xử lý sự kiện ---

        // Chọn ảnh avatar
        btnSelectAvatar.setOnClickListener(v -> checkPermissionAndPickImage());

        // Chọn ngày sinh
        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog());

        // Lưu người dùng
        btnSaveUser.setOnClickListener(v -> saveUser());

        // Khởi tạo ActivityResultLauncher cho cấp quyền
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Khởi tạo ActivityResultLauncher cho chọn ảnh
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        ivAvatar.setImageBitmap(bitmap);
                    } catch (IOException e) {
                        Log.e(TAG, "Lỗi khi tải ảnh: " + e.getMessage());
                        Toast.makeText(this, "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * Tải thông tin người dùng nếu đang ở chế độ chỉnh sửa
     */
    private void loadUserDetailsForEdit(String username) {
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    userToEdit = user; // <-- LƯU USER VÀO BIẾN MỚI
                    runOnUiThread(() -> {
                        // Hiển thị dữ liệu lên UI
                        etUsername.setText(userToEdit.getUsername());
                        etPassword.setText(userToEdit.getPassword());
                        etEmail.setText(userToEdit.getEmail());
                        etPhoneNumber.setText(userToEdit.getPhoneNumber());
                        etDateOfBirth.setText(userToEdit.getDateOfBirth());
                        if (userToEdit.getAvatar() != null && !userToEdit.getAvatar().isEmpty()) {
                            Glide.with(AddEditUserActivity.this)
                                    .load(userToEdit.getAvatar())
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .into(ivAvatar);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditUserActivity.this, "Không tìm thấy người dùng để chỉnh sửa", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải thông tin user để chỉnh sửa: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * Hiển thị DatePickerDialog để chọn ngày sinh
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    c.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etDateOfBirth.setText(sdf.format(c.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Kiểm tra quyền truy cập bộ nhớ và mở trình chọn ảnh
     */
    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openImagePicker();
        }
    }

    /**
     * Mở trình chọn ảnh từ thư viện
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    /**
     * Xử lý lưu người dùng (thêm mới hoặc cập nhật)
     */


    /**
     * Hàm thực hiện lưu (thêm/cập nhật) người dùng vào Supabase
     * @param user Đối tượng User cần lưu
     */

    private void saveUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Username, Password và Email không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu là chế độ thêm mới, tạo đối tượng mới
        // Nếu là chế độ sửa, sử dụng đối tượng userToEdit đã tải
        final User userToSave = (currentUsername == null) ? new User() : userToEdit;

        // Cập nhật thông tin từ form vào đối tượng
        userToSave.setUsername(username);
        userToSave.setPassword(password);
        userToSave.setEmail(email);
        userToSave.setPhoneNumber(phoneNumber);
        userToSave.setDateOfBirth(dateOfBirth);

        // Xử lý upload avatar nếu có ảnh mới được chọn
        if (selectedImageUri != null) {
            Bitmap bitmap = ((BitmapDrawable) ivAvatar.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();

            // Upload và sau đó thực hiện lưu thông tin user
            userService.uploadAvatar(username + "_avatar.jpg", imageData, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String avatarUrl) {
                    userToSave.setAvatar(avatarUrl); // Cập nhật link avatar mới
                    executeSaveOrUpdate(userToSave);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Lỗi khi upload avatar: ", e);
                    runOnUiThread(() -> Toast.makeText(AddEditUserActivity.this, "Lỗi upload avatar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    // Không lưu user nếu upload ảnh thất bại để tránh mất dữ liệu
                }
            });
        } else {
            // Nếu không chọn ảnh mới, thực hiện lưu luôn
            // (avatar cũ đã có sẵn trong userToEdit nếu là chế độ sửa)
            executeSaveOrUpdate(userToSave);
        }
    }

    private void executeSaveOrUpdate(User user) {
        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu user: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditUserActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        };

        if (currentUsername == null) { // Chế độ thêm mới
            userService.insertUser(user, callback);
        } else { // Chế độ cập nhật
            userService.updateUser(currentUsername, user, callback);
        }
    }
}