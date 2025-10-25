package com.ganshapebattle; // Hoặc package com.ganshapebattle.admin tùy thuộc vào vị trí tệp của bạn

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap; // Import Bitmap
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
// Import ImageUtils
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.R;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddEditUserActivity extends AppCompatActivity {

    private static final String TAG = "AddEditUserActivity";

    private TextView tvTitle;
    private CircleImageView ivAvatar;
    private Button btnSelectAvatar, btnSaveUser;
    private EditText etUsername, etPassword, etEmail, etPhoneNumber, etDateOfBirth;

    private UserService userService;
    private String currentUsername = null;
    private Uri selectedImageUri;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private User userToEdit;

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

        Intent intent = getIntent();
        if (intent.hasExtra("USER_USERNAME_EDIT")) {
            currentUsername = intent.getStringExtra("USER_USERNAME_EDIT");
            tvTitle.setText("Chỉnh sửa người dùng");
            etUsername.setEnabled(false);
            loadUserDetailsForEdit(currentUsername);
        } else {
            tvTitle.setText("Thêm người dùng mới");
        }

        btnSelectAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog());
        btnSaveUser.setOnClickListener(v -> saveUser());

        setupLaunchers();
    }

    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

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

    private void loadUserDetailsForEdit(String username) {
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    userToEdit = user;
                    runOnUiThread(() -> {
                        etUsername.setText(userToEdit.getUsername());
                        etPassword.setText(userToEdit.getPassword()); // Cân nhắc không hiển thị password cũ
                        etEmail.setText(userToEdit.getEmail());
                        etPhoneNumber.setText(userToEdit.getPhoneNumber());
                        etDateOfBirth.setText(userToEdit.getDateOfBirth());
                        if (userToEdit.getAvatar() != null && !userToEdit.getAvatar().isEmpty()) {
                            // Kiểm tra xem avatar là Base64 hay URL cũ
                            if (userToEdit.getAvatar().startsWith("http")) {
                                // Nếu là URL, dùng Glide
                                Glide.with(AddEditUserActivity.this)
                                        .load(userToEdit.getAvatar())
                                        .placeholder(R.drawable.ic_default_avatar)
                                        .error(R.drawable.ic_default_avatar)
                                        .into(ivAvatar);
                            } else {
                                // Nếu là Base64, giải mã và hiển thị
                                Bitmap avatarBitmap = ImageUtils.base64ToBitmap(userToEdit.getAvatar());
                                if (avatarBitmap != null) {
                                    ivAvatar.setImageBitmap(avatarBitmap);
                                } else {
                                    ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                                }
                            }
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditUserActivity.this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải thông tin user: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

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

    private void checkPermissionAndPickImage() {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        // Trên Android 13+, dùng READ_MEDIA_IMAGES
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

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

        final User userToSave = (currentUsername == null) ? new User() : userToEdit;

        userToSave.setUsername(username);
        // Chỉ cập nhật password nếu người dùng nhập password mới (hoặc khi tạo mới)
        if (!password.isEmpty() || currentUsername == null) {
            userToSave.setPassword(password); // Cân nhắc mã hóa mật khẩu
        }
        userToSave.setEmail(email);
        userToSave.setPhoneNumber(phoneNumber);
        userToSave.setDateOfBirth(dateOfBirth);

        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = ((BitmapDrawable) ivAvatar.getDrawable()).getBitmap();
                // Sử dụng PNG cho avatar để giữ chất lượng
                String base64Image = ImageUtils.bitmapToBase64(bitmap, Bitmap.CompressFormat.PNG, 100);

                if (base64Image != null) {
                    userToSave.setAvatar(base64Image); // Lưu chuỗi Base64
                    executeSaveOrUpdate(userToSave);
                } else {
                    Toast.makeText(this, "Không thể chuyển đổi ảnh thành Base64", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi xử lý ảnh thành Base64: ", e);
                Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Không có ảnh mới được chọn, giữ nguyên avatar cũ (nếu có)
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