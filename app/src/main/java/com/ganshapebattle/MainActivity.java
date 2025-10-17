package com.ganshapebattle;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Khai báo các thành phần UI
    private EditText usernameInput, passwordInput, emailInput;
    private Button addUserButton, updateUserButton, deleteUserButton, getAllUsersButton, getUserByIdButton;
    private TextView resultTextView;

    // Khai báo Service
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Service
        userService = new UserService();

        // Ánh xạ UI
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailInput = findViewById(R.id.emailInput);
        addUserButton = findViewById(R.id.addUserButton);
        updateUserButton = findViewById(R.id.updateUserButton);
        deleteUserButton = findViewById(R.id.deleteUserButton);
        getAllUsersButton = findViewById(R.id.getAllUsersButton);
        getUserByIdButton = findViewById(R.id.getUserByIdButton);
        resultTextView = findViewById(R.id.resultTextView);

        // Cài đặt sự kiện click cho các nút
        setupButtonClickListeners();

        // Tải danh sách user lần đầu khi mở app
        fetchAndDisplayAllUsers();
    }

    private void setupButtonClickListeners() {
        getAllUsersButton.setOnClickListener(v -> fetchAndDisplayAllUsers());

        getUserByIdButton.setOnClickListener(v -> handleGetUserById());

        addUserButton.setOnClickListener(v -> handleAddUser());

        updateUserButton.setOnClickListener(v -> handleUpdateUser());

        deleteUserButton.setOnClickListener(v -> handleDeleteUser());
    }

    // --- Các hàm xử lý logic ---

    private void fetchAndDisplayAllUsers() {
        resultTextView.setText("Đang tải danh sách...");
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> userList) {
                runOnUiThread(() -> {
                    if (userList != null && !userList.isEmpty()) {
                        StringBuilder displayText = new StringBuilder();
                        displayText.append("Tìm thấy ").append(userList.size()).append(" người dùng:\n\n");
                        for (User user : userList) {
                            displayText.append("- Username: ").append(user.getUsername()).append("\n");
                        }
                        resultTextView.setText(displayText.toString());
                    } else {
                        resultTextView.setText("Không tìm thấy người dùng nào.");
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    resultTextView.setText("Lỗi khi tải danh sách:\n" + e.getMessage());
                    Log.e(TAG, "getAllUsers failed: ", e);
                });
            }
        });
    }

    private void handleGetUserById() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Username", Toast.LENGTH_SHORT).show();
            return;
        }
        resultTextView.setText("Đang tìm " + username + "...");
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        String userDetails = "Tìm thấy User:\n" +
                                "- Username: " + user.getUsername() + "\n" +
                                "- Email: " + user.getEmail();
                        resultTextView.setText(userDetails);
                    } else {
                        resultTextView.setText("Không tìm thấy user với username: " + username);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> resultTextView.setText("Lỗi khi tìm User:\n" + e.getMessage()));
            }
        });
    }

    private void handleAddUser() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username và Password không được trống", Toast.LENGTH_SHORT).show();
            return;
        }

        User newUser = new User(username, password, email, null, null, null);
        userService.insertUser(newUser, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    fetchAndDisplayAllUsers(); // Tải lại danh sách sau khi thêm
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Thêm thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleUpdateUser() {
        String username = usernameInput.getText().toString().trim();
        String newPassword = passwordInput.getText().toString().trim();
        String newEmail = emailInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Username của người dùng cần cập nhật", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng User chỉ với những thông tin cần cập nhật
        User updatedInfo = new User();
        if (!newPassword.isEmpty()) updatedInfo.setPassword(newPassword);
        if (!newEmail.isEmpty()) updatedInfo.setEmail(newEmail);

        userService.updateUser(username, updatedInfo, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    fetchAndDisplayAllUsers(); // Tải lại danh sách
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void handleDeleteUser() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Username cần xóa", Toast.LENGTH_SHORT).show();
            return;
        }

        userService.deleteUser(username, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    fetchAndDisplayAllUsers(); // Tải lại danh sách
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Xóa thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}