// File: main/java/com/ganshapebattle/admin/UserCRUDActivity.java
package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R; //
import com.ganshapebattle.models.User; //
import com.ganshapebattle.services.SupabaseCallback; //
import com.ganshapebattle.services.UserService; //

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserCRUDActivity extends AppCompatActivity {

    private static final String TAG = "UserCRUDActivity";

    private ListView lvUsers;
    private Button btnAddUser;
    private SearchView searchView;
    private UserService userService;
    private ArrayAdapter<String> adapter;

    private final List<User> displayedUserList = new ArrayList<>();
    private final List<User> fullUserList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_crud); //

        lvUsers = findViewById(R.id.lvUsers); //
        btnAddUser = findViewById(R.id.btnAddUser); //
        searchView = findViewById(R.id.searchViewPictures);
        userService = new UserService(); //

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvUsers.setAdapter(adapter);

        setupSearch();

        // Khởi tạo Launcher để nhận kết quả từ AddEditUserActivity (khi thêm mới hoặc sửa từ detail)
        addEditUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Nhận được kết quả OK từ AddEditUserActivity.");
                        // onResume sẽ tự động gọi loadUsers()
                    } else {
                        Log.d(TAG, "AddEditUserActivity không trả về RESULT_OK.");
                    }
                }
        );

        // Mở màn hình CHI TIẾT khi nhấn vào item
        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayedUserList.size()) {
                User selectedUser = displayedUserList.get(position);
                Intent intent = new Intent(UserCRUDActivity.this, UserDetailActivity.class); // <<< Mở UserDetailActivity
                intent.putExtra("USER_USERNAME", selectedUser.getUsername()); // Truyền username
                startActivity(intent); // Dùng startActivity thông thường
                // Lưu ý: UserDetailActivity cần dùng launcher để mở AddEditUserActivity nếu nhấn Cập nhật
            } else {
                Log.e(TAG, "Vị trí item không hợp lệ: " + position);
            }
        });

        // Mở màn hình THÊM MỚI khi nhấn nút Add (dùng launcher)
        btnAddUser.setOnClickListener(v -> {
            Log.d(TAG, "Nhấn nút Thêm người dùng mới.");
            Intent intent = new Intent(UserCRUDActivity.this, AddEditUserActivity.class); //
            addEditUserLauncher.launch(intent);
        });

    } // Kết thúc onCreate

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại danh sách người dùng.");
        loadUsers(); // Tải lại dữ liệu khi quay lại màn hình
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterUsers(newText); return true; }
        });
        searchView.setOnCloseListener(() -> { filterUsers(""); return false; });
    }

    private void loadUsers() {
        Log.d(TAG, "Bắt đầu tải danh sách người dùng...");
        userService.getAllUsers(new SupabaseCallback<List<User>>() { //
            @Override
            public void onSuccess(List<User> result) {
                Log.d(TAG, "Tải danh sách người dùng thành công, số lượng: " + (result != null ? result.size() : 0));
                runOnUiThread(() -> {
                    fullUserList.clear();
                    if (result != null) { fullUserList.addAll(result); }
                    filterUsers(searchView.getQuery().toString()); // Cập nhật hiển thị
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải danh sách user: ", e);
                runOnUiThread(() -> Toast.makeText(UserCRUDActivity.this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void filterUsers(String query) {
        List<User> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(fullUserList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = fullUserList.stream()
                    .filter(user -> user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateDisplayedUsers(filteredList);
    }

    private void updateDisplayedUsers(List<User> users) {
        displayedUserList.clear();
        displayedUserList.addAll(users);
        List<String> displayItems = displayedUserList.stream()
                .map(user -> user.getUsername() + (user.getEmail() != null ? " (" + user.getEmail() + ")" : ""))
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(displayItems);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter đã được cập nhật với " + displayItems.size() + " items.");
    }
}