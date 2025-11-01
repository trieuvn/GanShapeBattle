// File: main/java/com/ganshapebattle/admin/GalleryCRUDActivity.java
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

// === SỬA LỖI 1: THAY ĐỔI IMPORT ===
import androidx.appcompat.widget.SearchView; // Dùng bản androidx
// ==================================

import com.ganshapebattle.R;
import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GalleryCRUDActivity extends AppCompatActivity {

    private static final String TAG = "GalleryCRUDActivity";

    private ListView lvGalleries;
    private Button btnAddGallery;

    // === SỬA LỖI 2: THAY ĐỔI KIỂU BIẾN ===
    private androidx.appcompat.widget.SearchView searchView; // Dùng bản androidx
    // ==================================

    private GalleryService galleryService;
    private ArrayAdapter<String> adapter;

    private final List<Gallery> displayedGalleryList = new ArrayList<>();
    private final List<Gallery> fullGalleryList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditGalleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_crud);

        lvGalleries = findViewById(R.id.lvGalleries);
        btnAddGallery = findViewById(R.id.btnAddGallery);

        // Dòng 48: Bây giờ đã an toàn, không cần ép kiểu (cast)
        searchView = findViewById(R.id.searchViewGalleries);

        galleryService = new GalleryService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvGalleries.setAdapter(adapter);

        setupSearch(); // Hàm này bây giờ sẽ hoạt động

        // (Phần còn lại của file giữ nguyên)
        addEditGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Nhận được kết quả OK từ AddEditGalleryActivity/GalleryDetailActivity.");
                        // onResume sẽ tự động gọi loadGalleries()
                    } else {
                        Log.d(TAG, "AddEditGalleryActivity/GalleryDetailActivity không trả về RESULT_OK.");
                    }
                }
        );

        lvGalleries.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayedGalleryList.size()) {
                Gallery selectedGallery = displayedGalleryList.get(position);
                Intent intent = new Intent(GalleryCRUDActivity.this, GalleryDetailActivity.class);
                intent.putExtra("GALLERY_ID", selectedGallery.getId());
                startActivity(intent);
            }
        });

        btnAddGallery.setOnClickListener(v -> {
            Intent intent = new Intent(GalleryCRUDActivity.this, AddEditGalleryActivity.class);
            addEditGalleryLauncher.launch(intent); // Dùng launcher
        });

    } // Kết thúc onCreate

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại danh sách galleries.");
        loadGalleries(); // Tải lại dữ liệu khi quay lại màn hình
    }

    private void setupSearch() {
        // Hàm này hoạt động tốt với androidx.appcompat.widget.SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterGalleries(newText); return true; }
        });
        searchView.setOnCloseListener(() -> { filterGalleries(""); return false; });
    }

    private void loadGalleries() {
        Log.d(TAG, "Bắt đầu tải danh sách galleries...");
        galleryService.getAllGalleries(new SupabaseCallback<List<Gallery>>() {
            @Override
            public void onSuccess(List<Gallery> result) {
                Log.d(TAG, "Tải galleries thành công: " + (result != null ? result.size() : 0));
                runOnUiThread(() -> {
                    fullGalleryList.clear();
                    if (result != null) { fullGalleryList.addAll(result); }
                    // Hàm getQuery() hoạt động tốt với androidx.appcompat.widget.SearchView
                    filterGalleries(searchView.getQuery().toString());
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải galleries: ", e);
//                runOnUiThread(() -> Toast.makeText(GalleryCRUDActivity.this, "Lỗi tải galleries", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterGalleries(String query) {
        List<Gallery> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(fullGalleryList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = fullGalleryList.stream()
                    .filter(gallery -> gallery.getName() != null && gallery.getName().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateDisplayedGalleries(filteredList);
    }

    private void updateDisplayedGalleries(List<Gallery> galleries) {
        displayedGalleryList.clear();
        displayedGalleryList.addAll(galleries);
        List<String> galleryNames = displayedGalleryList.stream()
                .map(gallery -> gallery.getName() != null ? gallery.getName() : "N/A")
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(galleryNames);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter galleries đã cập nhật.");
    }
}