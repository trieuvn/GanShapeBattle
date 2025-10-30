// File: main/java/com/ganshapebattle/ProfileActivity.java
package com.ganshapebattle;

import android.Manifest; //
import android.app.DatePickerDialog;
import android.content.Intent; //
import android.content.pm.PackageManager; //
import android.graphics.Bitmap; //
import android.graphics.drawable.Drawable; //
import android.net.Uri; //
import android.os.Build; //
import android.os.Bundle;
import android.os.Handler; //
import android.os.Looper; //
import android.provider.MediaStore; //
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; //
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; //

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ganshapebattle.models.Lobby; //
import com.ganshapebattle.models.User; //
import com.ganshapebattle.services.LobbyService; //
import com.ganshapebattle.services.SupabaseCallback; //
import com.ganshapebattle.services.UserService; //
import com.ganshapebattle.utils.ImageUtils; //

import java.io.IOException; //
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List; //
import java.util.Locale;
import java.util.concurrent.ExecutorService; //
import java.util.concurrent.Executors; //

// <<< Import FloatingActionButton >>>
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import de.hdodenhof.circleimageview.CircleImageView; //

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    // Khai báo các thành phần giao diện và service
    private TextView tvProfileEmail;
    private EditText etProfileUsername, etProfilePhoneNumber, etProfileDateOfBirth;
    // === SỬA LỖI Ở ĐÂY ===
    private Button btnSaveChanges;
    private FloatingActionButton btnSelectAvatar; // <<< Đổi từ Button sang FloatingActionButton
    // ======================
    private CircleImageView ivProfileAvatar;
    private UserService userService;
    private LobbyService lobbyService;
    private String currentUserEmail;
    private User currentUserData;
    private boolean isLobbyAdmin = false;

    // Biến cho việc chọn ảnh mới
    private Uri selectedImageUri = null;
    private String newAvatarBase64 = null;
    private boolean isSaving = false;

    // Launchers cho quyền và chọn ảnh
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Executor để xử lý Base64
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); //

        // --- Ánh xạ View ---
        tvProfileEmail = findViewById(R.id.tvProfileEmail); //
        etProfileUsername = findViewById(R.id.etProfileUsername); //
        etProfilePhoneNumber = findViewById(R.id.etProfilePhoneNumber); //
        etProfileDateOfBirth = findViewById(R.id.etProfileDateOfBirth); //
        btnSaveChanges = findViewById(R.id.btnSaveChanges); //
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar); //
        // === SỬA LỖI Ở ĐÂY ===
        btnSelectAvatar = findViewById(R.id.btnSelectAvatar); // <<< Ánh xạ đúng kiểu FloatingActionButton
        // ======================
        // --- ---

        userService = new UserService(); //
        lobbyService = new LobbyService(); //
        currentUserEmail = getIntent().getStringExtra("USER_EMAIL");

        if (currentUserEmail == null || currentUserEmail.isEmpty()) {
            Log.e(TAG, "Email người dùng không được truyền sang hoặc rỗng.");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvProfileEmail.setText(currentUserEmail);
        setupLaunchers();
        loadUserProfile();

        // --- Gắn sự kiện ---
        etProfileDateOfBirth.setOnClickListener(v -> showDatePickerDialog());
        btnSelectAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSaveChanges.setOnClickListener(v -> {
            if (!isSaving) {
                if (isLobbyAdmin && currentUserData != null &&
                        !etProfileUsername.getText().toString().trim().equals(currentUserData.getUsername())) {
                    Toast.makeText(this, "Bạn là admin phòng, không thể đổi username.", Toast.LENGTH_LONG).show();
                } else {
                    saveChanges();
                }
            }
        });
    }

    /**
     * Khởi tạo các ActivityResultLauncher để xử lý quyền và chọn ảnh.
     */
    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "Quyền đọc ảnh đã được cấp.");
                openImagePicker();
            } else {
                Log.w(TAG, "Quyền đọc ảnh bị từ chối.");
                Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    Log.d(TAG, "Ảnh đã được chọn: " + selectedImageUri);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        ivProfileAvatar.setImageBitmap(bitmap); // Hiển thị ngay
                        Log.d(TAG, "Đã cập nhật ImageView với ảnh mới chọn.");

                        Log.d(TAG, "Bắt đầu chuyển đổi ảnh mới sang Base64...");
                        executor.execute(() -> {
                            String base64Result = ImageUtils.bitmapToBase64(bitmap, Bitmap.CompressFormat.PNG, 90); //
                            handler.post(() -> {
                                if (base64Result != null) {
                                    newAvatarBase64 = base64Result;
                                    Log.d(TAG, "Chuyển đổi ảnh mới sang Base64 thành công.");
                                } else {
                                    newAvatarBase64 = null;
                                    Log.e(TAG, "Lỗi chuyển đổi ảnh mới sang Base64.");
                                    Toast.makeText(ProfileActivity.this, "Lỗi xử lý ảnh đại diện mới", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                    } catch (IOException e) {
                        Log.e(TAG, "Lỗi khi tải hoặc xử lý ảnh đã chọn: ", e);
                        Toast.makeText(this, "Không thể tải hoặc xử lý ảnh", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null;
                        newAvatarBase64 = null;
                    }
                }
            } else {
                Log.d(TAG, "Không chọn ảnh nào hoặc có lỗi.");
            }
        });
    }

    /**
     * Tải thông tin hồ sơ của người dùng hiện tại và kiểm tra quyền admin lobby.
     */
    private void loadUserProfile() {
        Log.d(TAG, "Đang tải hồ sơ cho: " + currentUserEmail);
        btnSaveChanges.setEnabled(false);

        userService.getUserByEmail(currentUserEmail, new SupabaseCallback<User>() { //
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Tải hồ sơ thành công.");
                    currentUserData = user;
                    runOnUiThread(() -> {
                        etProfileUsername.setText(user.getUsername());
                        etProfilePhoneNumber.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                        etProfileDateOfBirth.setText(user.getDateOfBirth() != null ? user.getDateOfBirth() : "");
                        String avatarData = user.getAvatar();
                        if (avatarData != null && !avatarData.isEmpty()) {
                            if (avatarData.startsWith("http")) {
                                Log.d(TAG, "Đang tải avatar từ URL: " + avatarData);
                                Glide.with(ProfileActivity.this).load(avatarData).placeholder(R.drawable.ic_default_avatar).error(R.drawable.ic_default_avatar).into(ivProfileAvatar); //
                            } else {
                                Log.d(TAG, "Đang giải mã avatar Base64...");
                                Bitmap avatarBitmap = ImageUtils.base64ToBitmap(avatarData); //
                                if (avatarBitmap != null) {
                                    ivProfileAvatar.setImageBitmap(avatarBitmap);
                                    Log.d(TAG, "Hiển thị avatar Base64 thành công.");
                                } else {
                                    ivProfileAvatar.setImageResource(R.drawable.ic_default_avatar); //
                                    Log.w(TAG, "Giải mã avatar Base64 thất bại.");
                                }
                            }
                        } else {
                            ivProfileAvatar.setImageResource(R.drawable.ic_default_avatar); //
                            Log.d(TAG, "Không có dữ liệu avatar.");
                        }
                    });
                    checkIfUserIsLobbyAdmin(user.getUsername());
                } else {
                    Log.w(TAG, "Không tìm thấy hồ sơ cho email: " + currentUserEmail);
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Không tìm thấy hồ sơ.", Toast.LENGTH_SHORT).show();
                        btnSaveChanges.setEnabled(false);
                    });
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải hồ sơ: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(false);
                });
            }
        });
    }

    /**
     * Kiểm tra xem username có phải là admin của bất kỳ lobby nào không.
     */
    private void checkIfUserIsLobbyAdmin(final String usernameToCheck) {
        if (usernameToCheck == null || usernameToCheck.isEmpty()) {
            isLobbyAdmin = false;
            runOnUiThread(() -> {
                etProfileUsername.setEnabled(true);
                btnSaveChanges.setEnabled(true);
            });
            return;
        }

        Log.d(TAG, "Kiểm tra xem '" + usernameToCheck + "' có phải là admin lobby không...");
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() { //
            @Override
            public void onSuccess(List<Lobby> lobbies) {
                boolean isAdmin = false;
                if (lobbies != null) {
                    for (Lobby lobby : lobbies) { //
                        if (usernameToCheck.equals(lobby.getAdminUsername())) {
                            isAdmin = true;
                            Log.d(TAG,"User '" + usernameToCheck + "' là admin của lobby ID: " + lobby.getId());
                            break;
                        }
                    }
                }
                isLobbyAdmin = isAdmin;
                Log.d(TAG, "Kết quả kiểm tra admin lobby: " + isLobbyAdmin);

                runOnUiThread(() -> {
                    if (isLobbyAdmin) {
                        etProfileUsername.setEnabled(false);
                        Toast.makeText(ProfileActivity.this, "Bạn là admin phòng, không thể đổi username.", Toast.LENGTH_SHORT).show();
                    } else {
                        etProfileUsername.setEnabled(true);
                    }
                    btnSaveChanges.setEnabled(true);
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi kiểm tra danh sách lobby: ", e);
                isLobbyAdmin = false;
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Lỗi kiểm tra quyền admin lobby.", Toast.LENGTH_SHORT).show();
                    etProfileUsername.setEnabled(true);
                    btnSaveChanges.setEnabled(true);
                });
            }
        });
    }

    /**
     * Hiển thị DatePickerDialog để chọn ngày sinh.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        String currentDate = etProfileDateOfBirth.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                c.setTime(sdf.parse(currentDate));
            } catch (Exception e) {
                Log.w(TAG, "Không thể parse ngày sinh hiện tại: " + currentDate);
            }
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etProfileDateOfBirth.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    /**
     * Kiểm tra quyền đọc bộ nhớ và yêu cầu nếu cần, sau đó mở thư viện ảnh.
     */
    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES; //
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE; //
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Yêu cầu quyền đọc ảnh...");
            requestPermissionLauncher.launch(permission);
        } else {
            Log.d(TAG, "Đã có quyền, mở thư viện ảnh...");
            openImagePicker();
        }
    }

    /**
     * Mở Intent để người dùng chọn ảnh từ thư viện.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    /**
     * Thu thập dữ liệu đã thay đổi, gọi API để cập nhật hồ sơ người dùng,
     * và trả kết quả về MainActivity.
     */
    private void saveChanges() {
        isSaving = true;
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Đang lưu...");

        final String newUsername = etProfileUsername.getText().toString().trim();
        String newPhoneNumber = etProfilePhoneNumber.getText().toString().trim();
        String newDateOfBirth = etProfileDateOfBirth.getText().toString().trim();

        if (newUsername.isEmpty()) {
            showErrorAndResetButton("Username không được để trống");
            return;
        }

        User updatedUserData = new User(); //
        boolean changed = false;

        if (!isLobbyAdmin && (currentUserData == null || !newUsername.equals(currentUserData.getUsername()))) {
            updatedUserData.setUsername(newUsername);
            changed = true;
            Log.d(TAG, "Username sẽ được cập nhật.");
        }

        String currentPhone = currentUserData != null ? currentUserData.getPhoneNumber() : null;
        String phoneToSave = newPhoneNumber.isEmpty() ? null : newPhoneNumber;
        if ( (currentPhone == null && phoneToSave != null) || (currentPhone != null && !currentPhone.equals(phoneToSave)) ) {
            updatedUserData.setPhoneNumber(phoneToSave);
            changed = true;
            Log.d(TAG, "Số điện thoại sẽ được cập nhật.");
        }

        String currentDOB = currentUserData != null ? currentUserData.getDateOfBirth() : null;
        String dobToSave = newDateOfBirth.isEmpty() ? null : newDateOfBirth;
        if ( (currentDOB == null && dobToSave != null) || (currentDOB != null && !currentDOB.equals(dobToSave)) ) {
            updatedUserData.setDateOfBirth(dobToSave);
            changed = true;
            Log.d(TAG, "Ngày sinh sẽ được cập nhật.");
        }

        String currentAvatar = currentUserData != null ? currentUserData.getAvatar() : null;
        if (newAvatarBase64 != null && !newAvatarBase64.equals(currentAvatar)) {
            updatedUserData.setAvatar(newAvatarBase64);
            changed = true;
            Log.d(TAG, "Avatar sẽ được cập nhật.");
        } else if (newAvatarBase64 == null && selectedImageUri != null) {
            Toast.makeText(this, "Ảnh đại diện mới có lỗi, sẽ không được lưu.", Toast.LENGTH_SHORT).show();
        }

        if (!changed) {
            showErrorAndResetButton("Không có thay đổi nào để lưu.");
            return;
        }

        Log.d(TAG, "Đang gọi updateUser cho email: " + currentUserEmail);
        userService.updateUser(currentUserEmail, updatedUserData, new SupabaseCallback<String>() { //
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Cập nhật hồ sơ thành công trên Supabase: " + result);
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    newAvatarBase64 = null;
                    selectedImageUri = null;
                    isSaving = false;

                    Intent resultIntent = new Intent();
                    if (!isLobbyAdmin && updatedUserData.getUsername() != null) {
                        resultIntent.putExtra("UPDATED_USERNAME", newUsername);
                    }
                    setResult(RESULT_OK, resultIntent);

                    finish(); // <<< Quay về MainActivity
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi cập nhật hồ sơ trên Supabase: ", e);
                showErrorAndResetButton("Lỗi cập nhật: " + e.getMessage());
            }
        });
    }

    /**
     * Hiển thị Toast lỗi và kích hoạt lại nút Lưu.
     */
    private void showErrorAndResetButton(String message) {
        runOnUiThread(() -> {
            Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show();
            isSaving = false;
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Lưu thay đổi");
        });
    }

    /**
     * Hủy executor khi Activity bị hủy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}