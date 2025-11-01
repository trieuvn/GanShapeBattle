// File: main/java/com/ganshapebattle/admin/PictureCRUDActivity.java
package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
//import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// === SỬA LỖI: THAY ĐỔI IMPORT ===
import androidx.appcompat.widget.SearchView;
// ================================

import com.ganshapebattle.R;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PictureCRUDActivity extends AppCompatActivity {

    private static final String TAG = "PictureCRUDActivity";

    private ListView lvPictures;
    private Button btnAddPicture;
    // === SỬA LỖI: THAY ĐỔI KIỂU BIẾN ===
    private androidx.appcompat.widget.SearchView searchView;
    // ==================================
    private PictureService pictureService;
    private ArrayAdapter<String> adapter;

    private final List<Picture> displayedPictureList = new ArrayList<>();
    private final List<Picture> fullPictureList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditPictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_crud);

        lvPictures = findViewById(R.id.lvPictures);
        btnAddPicture = findViewById(R.id.btnAddPicture);
        searchView = findViewById(R.id.searchViewLobbies);
        pictureService = new PictureService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvPictures.setAdapter(adapter);

        setupSearch();

        // Khởi tạo Launcher
        addEditPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Nhận được kết quả OK từ AddEditPictureActivity/PictureDetailActivity.");
                        // onResume sẽ tự động gọi loadPictures()
                    } else {
                        Log.d(TAG, "AddEditPictureActivity/PictureDetailActivity không trả về RESULT_OK.");
                    }
                }
        );

        // Mở màn hình CHI TIẾT khi nhấn vào item
        lvPictures.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayedPictureList.size()) {
                Picture selectedPicture = displayedPictureList.get(position);
                Intent intent = new Intent(PictureCRUDActivity.this, PictureDetailActivity.class);
                intent.putExtra("PICTURE_ID", selectedPicture.getId());
                startActivity(intent);
            }
        });

        // Mở màn hình THÊM MỚI khi nhấn nút Add
        btnAddPicture.setOnClickListener(v -> {
            Intent intent = new Intent(PictureCRUDActivity.this, AddEditPictureActivity.class);
            addEditPictureLauncher.launch(intent); // Dùng launcher
        });

    } // Kết thúc onCreate

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại danh sách pictures.");
        loadPictures(); // Tải lại dữ liệu khi quay lại màn hình
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterPictures(newText); return true; }
        });
        searchView.setOnCloseListener(() -> { filterPictures(""); return false; });
    }

    private void loadPictures() {
        Log.d(TAG, "Bắt đầu tải danh sách pictures...");
        pictureService.getAllPictures(new SupabaseCallback<List<Picture>>() {
            @Override
            public void onSuccess(List<Picture> result) {
                Log.d(TAG, "Tải pictures thành công: " + (result != null ? result.size() : 0));
                runOnUiThread(() -> {
                    fullPictureList.clear();
                    if (result != null) { fullPictureList.addAll(result); }
                    filterPictures(searchView.getQuery().toString());
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải pictures: ", e);
//                runOnUiThread(() -> Toast.makeText(PictureCRUDActivity.this, "Lỗi tải hình ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void filterPictures(String query) {
        List<Picture> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(fullPictureList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = fullPictureList.stream()
                    .filter(picture -> picture.getName() != null && picture.getName().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateDisplayedPictures(filteredList);
    }

    private void updateDisplayedPictures(List<Picture> pictures) {
        displayedPictureList.clear();
        displayedPictureList.addAll(pictures);
        List<String> pictureNames = displayedPictureList.stream()
                .map(picture -> picture.getName() != null ? picture.getName() : "N/A")
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(pictureNames);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter pictures đã cập nhật.");
    }
}