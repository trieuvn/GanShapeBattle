package com.ganshapebattle.admin; // Thay đổi thành package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView; // Import SearchView
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserCRUDActivity extends AppCompatActivity {

    private static final String TAG = "UserCRUDActivity";

    private ListView lvUsers;
    private Button btnAddUser;
    private SearchView searchView; // <-- Thêm biến cho SearchView
    private UserService userService;
    private ArrayAdapter<String> adapter;

    // Danh sách để hiển thị và danh sách đầy đủ để lọc
    private List<User> displayedUserList = new ArrayList<>();
    private List<User> fullUserList = new ArrayList<>(); // <-- Danh sách đầy đủ

    private ActivityResultLauncher<Intent> addEditUserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_crud);

        // Ánh xạ các view
        lvUsers = findViewById(R.id.lvUsers);
        btnAddUser = findViewById(R.id.btnAddUser);
        searchView = findViewById(R.id.searchView); // <-- Ánh xạ SearchView
        userService = new UserService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvUsers.setAdapter(adapter);

        // Tải danh sách user
        loadUsers();
        // Cài đặt listener cho thanh tìm kiếm
        setupSearch();

        // Xử lý sự kiện khi nhấn vào một item trong ListView
        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = displayedUserList.get(position); // Lấy user từ danh sách đang hiển thị
            Intent intent = new Intent(UserCRUDActivity.this, UserDetailActivity.class);
            intent.putExtra("USER_USERNAME", selectedUser.getUsername());
            startActivity(intent);
        });

        // Mở AddEditUserActivity để thêm người dùng mới
        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserCRUDActivity.this, AddEditUserActivity.class);
            addEditUserLauncher.launch(intent);
        });

        // Khởi tạo ActivityResultLauncher
        addEditUserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadUsers(); // Tải lại danh sách sau khi thêm/sửa thành công
                    }
                }
        );
    }

    /**
     * Cài đặt listener cho SearchView để xử lý việc lọc danh sách
     */
    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Không cần xử lý khi nhấn submit, vì ta lọc real-time
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Lọc danh sách mỗi khi người dùng thay đổi văn bản tìm kiếm
                filterUsers(newText);
                return true;
            }
        });
    }

    /**
     * Tải danh sách tất cả người dùng từ Supabase
     */
    private void loadUsers() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                runOnUiThread(() -> {
                    // Lưu vào cả hai danh sách
                    fullUserList.clear();
                    fullUserList.addAll(result);
                    updateDisplayedUsers(fullUserList); // Cập nhật danh sách hiển thị
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải danh sách user: ", e);
                runOnUiThread(() -> Toast.makeText(UserCRUDActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Lọc danh sách người dùng dựa trên chuỗi tìm kiếm
     * @param query Chuỗi ký tự để lọc
     */
    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            // Nếu không có gì trong ô tìm kiếm, hiển thị lại danh sách đầy đủ
            updateDisplayedUsers(fullUserList);
        } else {
            // Lọc danh sách đầy đủ để tìm các user có username chứa chuỗi tìm kiếm
            // (không phân biệt chữ hoa/thường)
            List<User> filteredList = fullUserList.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            updateDisplayedUsers(filteredList);
        }
    }

    /**
     * Cập nhật lại ListView với danh sách người dùng mới
     * @param users Danh sách người dùng cần hiển thị
     */
    private void updateDisplayedUsers(List<User> users) {
        // Cập nhật danh sách đang hiển thị
        displayedUserList.clear();
        displayedUserList.addAll(users);

        // Lấy danh sách username để đưa vào adapter
        List<String> usernames = displayedUserList.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        adapter.clear();
        adapter.addAll(usernames);
        adapter.notifyDataSetChanged();
    }
}