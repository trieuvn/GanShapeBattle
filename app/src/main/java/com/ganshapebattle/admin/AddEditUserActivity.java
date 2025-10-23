// File: main/java/com/ganshapebattle/admin/AddEditUserActivity.java
package com.ganshapebattle.admin; // Hoặc package com.ganshapebattle.admin tùy thuộc vào vị trí tệp của bạn

import android.Manifest; //
import android.app.DatePickerDialog;
import android.content.Intent; //
import android.content.pm.PackageManager; //
import android.graphics.Bitmap; // Import Bitmap
import android.graphics.drawable.BitmapDrawable; // Import BitmapDrawable
import android.net.Uri; //
import android.os.Build; // <<< Import Build
import android.os.Bundle;
import android.provider.MediaStore; //
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // <<< Import NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; //

import com.bumptech.glide.Glide;
// Import ImageUtils
import com.ganshapebattle.utils.ImageUtils; //
import com.ganshapebattle.R; //
import com.ganshapebattle.models.Lobby; //
import com.ganshapebattle.models.User; //
import com.ganshapebattle.services.LobbyService; //
import com.ganshapebattle.services.SupabaseCallback; //
import com.ganshapebattle.services.UserService; //

import java.io.IOException; //
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List; // <<< Import List
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddEditUserActivity extends AppCompatActivity {

    private static final String TAG = "AddEditUserActivity";

    // Khai báo các thành phần giao diện
    private TextView tvTitle;
    private CircleImageView ivAvatar;
    private Button btnSelectAvatar, btnSaveUser;
    // === BỎ EditText MẬT KHẨU ===
    // private EditText etPassword;
    // ============================
    private EditText etUsername, etEmail, etPhoneNumber, etDateOfBirth;

    // Service để tương tác với Supabase
    private UserService userService;
    private LobbyService lobbyService; //

    // Biến lưu trữ thông tin user đang được chỉnh sửa
    private String currentUsername = null; // Username gốc (dùng để lấy dữ liệu ban đầu và kiểm tra thay đổi)
    private String currentUserEmail = null; // Email (dùng để gọi API updateUser)
    private User userToEdit; // Lưu user gốc để tham khảo
    private boolean isTargetUserLobbyAdmin = false; // <<< Cờ kiểm tra user đang sửa có phải admin lobby không

    // Biến cho việc chọn ảnh mới
    private Uri selectedImageUri;
    private String newAvatarBase64 = null; // Lưu Base64 ảnh mới

    // Launchers để xử lý quyền và kết quả chọn ảnh
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_user); //

        // Ánh xạ View từ layout
        tvTitle = findViewById(R.id.tvTitle); //
        ivAvatar = findViewById(R.id.ivAvatar); //
        btnSelectAvatar = findViewById(R.id.btnSelectAvatar); //
        btnSaveUser = findViewById(R.id.btnSaveUser); //
        etUsername = findViewById(R.id.etUsername); //
        // === BỎ ÁNH XẠ MẬT KHẨU ===
        // etPassword = findViewById(R.id.etPassword); // Đã xóa khỏi layout
        // =========================
        etEmail = findViewById(R.id.etEmail); //
        etPhoneNumber = findViewById(R.id.etPhoneNumber); //
        etDateOfBirth = findViewById(R.id.etDateOfBirth); //

        userService = new UserService(); //
        lobbyService = new LobbyService(); // <<< Khởi tạo LobbyService
        setupLaunchers(); // Khởi tạo các ActivityResultLauncher trước

        Intent intent = getIntent();
        // Kiểm tra xem Activity được mở ở chế độ chỉnh sửa hay thêm mới
        if (intent.hasExtra("USER_USERNAME_EDIT")) {
            currentUsername = intent.getStringExtra("USER_USERNAME_EDIT");
            tvTitle.setText("Chỉnh sửa người dùng");
            // Không disable username nữa etUsername.setEnabled(false);
            etEmail.setEnabled(false);   // Không cho sửa email vì dùng để login và liên kết Auth
            Log.d(TAG, "Chế độ chỉnh sửa cho username: " + currentUsername);
            loadUserDetailsForEdit(currentUsername); // Tải dữ liệu user cần sửa (sẽ gọi kiểm tra lobby admin)
        } else {
            tvTitle.setText("Thêm người dùng mới");
            etEmail.setEnabled(true); // Cho phép nhập email khi thêm mới
            Log.d(TAG, "Chế độ thêm người dùng mới.");
            // Khi thêm mới, mặc định không phải admin lobby và cho sửa username
            etUsername.setEnabled(true);
            btnSaveUser.setEnabled(true); // Đảm bảo nút Save được bật
        }

        // Thiết lập sự kiện click cho các nút và EditText ngày sinh
        btnSelectAvatar.setOnClickListener(v -> checkPermissionAndPickImage());
        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog());
        btnSaveUser.setOnClickListener(v -> saveUser()); // Hàm saveUser sẽ kiểm tra cờ isTargetUserLobbyAdmin

    }

    /**
     * Khởi tạo các ActivityResultLauncher để xử lý việc xin quyền và chọn ảnh.
     */
    private void setupLaunchers() {
        // Launcher xin quyền truy cập bộ nhớ/ảnh
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Log.d(TAG, "Quyền đọc ảnh đã được cấp.");
                openImagePicker(); // Nếu được cấp quyền, mở thư viện ảnh
            } else {
                Log.w(TAG, "Quyền đọc ảnh bị từ chối.");
                Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Launcher mở thư viện ảnh và nhận ảnh được chọn
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedImageUri = result.getData().getData(); // Lấy Uri ảnh
                if (selectedImageUri != null) {
                    Log.d(TAG, "Ảnh đã được chọn từ thư viện: " + selectedImageUri);
                    try {
                        // Lấy Bitmap từ Uri
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        // Hiển thị ảnh lên ImageView
                        ivAvatar.setImageBitmap(bitmap);
                        Log.d(TAG, "Đã hiển thị ảnh mới chọn lên ImageView.");
                        // Chuyển đổi Bitmap thành chuỗi Base64 (dùng PNG để giữ chất lượng)
                        // Nên chạy trên background thread nếu ảnh lớn, nhưng tạm thời làm trực tiếp
                        newAvatarBase64 = ImageUtils.bitmapToBase64(bitmap, Bitmap.CompressFormat.PNG, 90); // Chất lượng 90
                        if (newAvatarBase64 == null) {
                            Log.e(TAG, "Lỗi chuyển đổi ảnh mới sang Base64.");
                            Toast.makeText(this, "Lỗi xử lý ảnh đại diện mới", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Ảnh mới đã được chuyển sang Base64.");
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Lỗi khi tải ảnh từ Uri: ", e);
                        Toast.makeText(this, "Không thể tải ảnh đã chọn", Toast.LENGTH_SHORT).show();
                        selectedImageUri = null; // Reset nếu lỗi
                        newAvatarBase64 = null;
                    }
                }
            } else {
                Log.d(TAG, "Không chọn ảnh nào hoặc Activity trả về lỗi.");
            }
        });
    }

    /**
     * Tải thông tin chi tiết của người dùng cần chỉnh sửa dựa vào username.
     * Sau đó gọi kiểm tra xem user có phải admin lobby không.
     * @param username Username của người dùng cần tải.
     */
    private void loadUserDetailsForEdit(String username) {
        Log.d(TAG, "Bắt đầu tải thông tin cho username: " + username);
        // Tạm thời vô hiệu hóa nút lưu và username field trong khi tải và kiểm tra
        btnSaveUser.setEnabled(false);
        etUsername.setEnabled(false); // Sẽ được bật lại nếu không phải admin lobby

        userService.getUserByUsername(username, new SupabaseCallback<User>() { //
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Tải thông tin user thành công: " + user.getEmail());
                    userToEdit = user; // Lưu lại thông tin user gốc
                    currentUserEmail = user.getEmail(); // <<< Lưu email vào biến thành viên để dùng khi update
                    runOnUiThread(() -> {
                        // Hiển thị dữ liệu lên các trường EditText và ImageView
                        etUsername.setText(userToEdit.getUsername());
                        // === BỎ HIỂN THỊ PASSWORD ===
                        // etPassword.setText("");
                        // etPassword.setHint("Nhập mật khẩu mới (nếu muốn thay đổi)");
                        // ============================
                        etEmail.setText(userToEdit.getEmail());
                        etPhoneNumber.setText(userToEdit.getPhoneNumber());
                        etDateOfBirth.setText(userToEdit.getDateOfBirth());

                        // Hiển thị avatar
                        String avatarData = userToEdit.getAvatar();
                        if (avatarData != null && !avatarData.isEmpty()) {
                            if (avatarData.startsWith("http")) { // Nếu là URL
                                Glide.with(AddEditUserActivity.this)
                                        .load(avatarData)
                                        .placeholder(R.drawable.ic_default_avatar) //
                                        .error(R.drawable.ic_default_avatar) //
                                        .into(ivAvatar);
                            } else { // Nếu là Base64
                                Bitmap avatarBitmap = ImageUtils.base64ToBitmap(avatarData); //
                                if (avatarBitmap != null) {
                                    ivAvatar.setImageBitmap(avatarBitmap);
                                } else {
                                    Log.w(TAG, "Không thể giải mã avatar Base64 khi tải.");
                                    ivAvatar.setImageResource(R.drawable.ic_default_avatar); //
                                }
                            }
                        } else {
                            Log.d(TAG, "User không có avatar khi tải.");
                            ivAvatar.setImageResource(R.drawable.ic_default_avatar); //
                        }
                    });
                    // <<< Gọi kiểm tra Lobby Admin SAU KHI có username >>>
                    checkIfTargetUserIsLobbyAdmin(user.getUsername());
                    // <<< >>>
                } else {
                    // Xử lý trường hợp không tìm thấy user
                    Log.w(TAG, "Không tìm thấy user với username: " + username);
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditUserActivity.this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        finish(); // Đóng Activity nếu không có dữ liệu
                    });
                }
            }
            @Override
            public void onFailure(Exception e) {
                // Xử lý lỗi tải user
                Log.e(TAG, "Lỗi tải thông tin user: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish(); // Đóng Activity nếu lỗi
                });
            }
        });
    }

    /**
     * Kiểm tra xem user đang được sửa có phải là admin lobby nào không.
     * Cập nhật cờ isTargetUserLobbyAdmin và trạng thái của EditText username, Button Save.
     */
    private void checkIfTargetUserIsLobbyAdmin(final String usernameToCheck) {
        if (usernameToCheck == null || usernameToCheck.isEmpty()) {
            // Nếu không có username (trường hợp hiếm), coi như không phải admin
            isTargetUserLobbyAdmin = false;
            runOnUiThread(() -> {
                etUsername.setEnabled(true); // Cho sửa
                btnSaveUser.setEnabled(true); // Cho lưu
            });
            Log.w(TAG,"Username để kiểm tra admin lobby là null hoặc rỗng.");
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
                            break; // Tìm thấy là đủ
                        }
                    }
                }
                isTargetUserLobbyAdmin = isAdmin; // Cập nhật cờ
                Log.d(TAG, "Kết quả kiểm tra admin lobby cho user đang sửa: " + isTargetUserLobbyAdmin);

                // Cập nhật UI trên Main thread
                runOnUiThread(() -> {
                    if (isTargetUserLobbyAdmin) {
                        etUsername.setEnabled(false); // <<< Vô hiệu hóa nếu là admin
                        Toast.makeText(AddEditUserActivity.this, "Người dùng này là admin phòng, không thể đổi username.", Toast.LENGTH_SHORT).show();
                    } else {
                        etUsername.setEnabled(true); // <<< Cho phép sửa nếu không phải
                    }
                    btnSaveUser.setEnabled(true); // Bật nút Lưu sau khi kiểm tra xong
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi kiểm tra danh sách lobby: ", e);
                isTargetUserLobbyAdmin = false; // Tạm coi là không phải nếu lỗi
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, "Lỗi kiểm tra quyền admin lobby.", Toast.LENGTH_SHORT).show();
                    etUsername.setEnabled(true); // Tạm cho phép sửa
                    btnSaveUser.setEnabled(true); // Bật nút Lưu
                });
            }
        });
    }


    /**
     * Hiển thị DatePickerDialog để người dùng chọn ngày sinh.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        String currentDate = etDateOfBirth.getText().toString();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                c.setTime(sdf.parse(currentDate));
            } catch (Exception e) {
                Log.w(TAG, "Không thể parse ngày sinh: " + currentDate);
            }
        }
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    c.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etDateOfBirth.setText(sdf.format(c.getTime())); // Cập nhật EditText
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()); // Không cho chọn ngày tương lai
        datePickerDialog.show();
    }

    /**
     * Kiểm tra quyền truy cập ảnh và mở thư viện nếu có quyền.
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
     * Mở Intent để chọn ảnh từ thư viện.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*"); // Chỉ cho phép chọn file ảnh
        pickImageLauncher.launch(intent); // Khởi chạy Activity chọn ảnh
    }

    /**
     * Thu thập dữ liệu (trừ mật khẩu), kiểm tra và gọi hàm lưu hoặc cập nhật.
     */
    private void saveUser() {
        // Lấy username mới từ EditText
        String username = etUsername.getText().toString().trim();
        // === BỎ LẤY MẬT KHẨU ===
        // String password = etPassword.getText().toString().trim();
        // =======================
        String email = etEmail.getText().toString().trim(); // Email gốc (không cho sửa khi edit)
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String dateOfBirth = etDateOfBirth.getText().toString().trim();


        // Kiểm tra admin lobby và username thay đổi (giữ nguyên)
        if (isTargetUserLobbyAdmin && currentUsername != null && !username.equals(currentUsername)) {
            Toast.makeText(this, "Không thể thay đổi username của admin phòng.", Toast.LENGTH_LONG).show();
            return;
        }

        // Kiểm tra username trống
        if (username.isEmpty()) {
            Toast.makeText(this, "Username không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        // Email chỉ bắt buộc khi thêm mới
        if (currentUsername == null && email.isEmpty()) {
            Toast.makeText(this, "Email không được để trống khi tạo người dùng mới", Toast.LENGTH_SHORT).show();
            return;
        }
        // === BỎ KIỂM TRA MẬT KHẨU ===
        // ============================


        User userToSave;
        if (currentUsername == null) { // Thêm mới
            Log.d(TAG,"Chuẩn bị dữ liệu cho insertUser");
            userToSave = new User(); //
            userToSave.setUsername(username);
            userToSave.setEmail(email);
            // === BỎ SET MẬT KHẨU ===
            // Lưu ý: Cần đảm bảo cột password trong DB cho phép NULL
            userToSave.setPassword(null); // Hoặc không set gì cả nếu mặc định là NULL
            // =======================
        } else { // Cập nhật
            Log.d(TAG,"Chuẩn bị dữ liệu cho updateUser");
            userToSave = new User();
            userToSave.setUsername(username); // Gửi username mới/cũ
            // === BỎ SET MẬT KHẨU ===
            // =======================
        }

        // Luôn gửi các trường còn lại
        userToSave.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
        userToSave.setDateOfBirth(dateOfBirth.isEmpty() ? null : dateOfBirth);

        // Xử lý avatar mới
        if (newAvatarBase64 != null) {
            userToSave.setAvatar(newAvatarBase64);
            Log.d(TAG,"Sử dụng avatar Base64 mới để lưu.");
        } else {
            Log.d(TAG,"Không có avatar Base64 mới.");
            // Khi cập nhật, UserService.updateUser sẽ không gửi trường avatar nếu null
        }

        executeSaveOrUpdate(userToSave); // Gọi hàm thực thi lưu/cập nhật
    }

    /**
     * Thực thi việc gọi API insert hoặc update của UserService.
     * Trả kết quả về Activity trước đó nếu thành công.
     */
    private void executeSaveOrUpdate(final User user) {
        btnSaveUser.setEnabled(false);
        btnSaveUser.setText("Đang lưu...");

        SupabaseCallback<String> callback = new SupabaseCallback<String>() { //
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, result, Toast.LENGTH_LONG).show();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("UPDATED_USERNAME", user.getUsername());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu user: ", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddEditUserActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSaveUser.setEnabled(true);
                    // Bật lại EditText username nếu không phải admin lobby
                    if (!isTargetUserLobbyAdmin) { etUsername.setEnabled(true); }
                    btnSaveUser.setText(currentUsername == null ? "Lưu người dùng" : "Lưu thay đổi");
                });
            }
        };

        if (currentUsername == null) { // Chế độ thêm mới
            Log.d(TAG, "Đang gọi insertUser...");
            // Lưu ý: Nếu cột password trong DB là NOT NULL, lệnh insert này sẽ lỗi.
            userService.insertUser(user, callback); //
        } else { // Chế độ cập nhật
            if (currentUserEmail != null && !currentUserEmail.isEmpty()) {
                Log.d(TAG, "Đang gọi updateUser cho email: " + currentUserEmail);
                userService.updateUser(currentUserEmail, user, callback); //
            } else {
                Log.e(TAG, "currentUserEmail là null hoặc rỗng, không thể cập nhật!");
                Toast.makeText(this, "Lỗi: Không tìm thấy email để cập nhật.", Toast.LENGTH_SHORT).show();
                btnSaveUser.setEnabled(true);
                if (!isTargetUserLobbyAdmin) etUsername.setEnabled(true);
                btnSaveUser.setText("Lưu thay đổi");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

} // Kết thúc class AddEditUserActivity