package com.ganshapebattle.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.UUID;

public class AddEditGalleryActivity extends AppCompatActivity {

    private static final String TAG = "AddEditGalleryActivity";

    private TextView tvTitle;
    private EditText etName, etDescription;
    private AutoCompleteTextView spinnerType;
    private Button btnSave;

    private GalleryService galleryService;
    private String currentGalleryId = null;
    private Gallery galleryToEdit;

    // Danh sách loại gallery
    private final String[] galleryTypes = {"Public", "Private", "Shared"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_gallery);

        // Ánh xạ view
        tvTitle = findViewById(R.id.tvGalleryTitle);
        etName = findViewById(R.id.etGalleryName);
        etDescription = findViewById(R.id.etGalleryDescription);
        spinnerType = findViewById(R.id.spinnerGalleryType);
        btnSave = findViewById(R.id.btnSaveGallery);

        // Thiết lập spinner (AutoCompleteTextView)
        setupSpinner();

        galleryService = new GalleryService();

        // Nhận ID từ Intent nếu chỉnh sửa
        currentGalleryId = getIntent().getStringExtra("GALLERY_ID_EDIT");

        if (currentGalleryId != null) {
            tvTitle.setText("Chỉnh sửa bộ sưu tập");
            loadGalleryDetails(currentGalleryId);
        } else {
            tvTitle.setText("Tạo bộ sưu tập mới");
        }

        // Lưu gallery
        btnSave.setOnClickListener(v -> saveGallery());
    }

    // Thiết lập adapter cho spinner
    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                galleryTypes
        );
        spinnerType.setAdapter(adapter);
    }

    // Tải dữ liệu gallery khi chỉnh sửa
    private void loadGalleryDetails(String galleryId) {
        galleryService.getGalleryById(galleryId, new SupabaseCallback<Gallery>() {
            @Override
            public void onSuccess(Gallery gallery) {
                if (gallery != null) {
                    galleryToEdit = gallery;
                    runOnUiThread(() -> {
                        etName.setText(gallery.getName());
                        etDescription.setText(gallery.getDescription());
                        spinnerType.setText(gallery.getType(), false);
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải chi tiết gallery: ", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditGalleryActivity.this, "Không thể tải dữ liệu", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // Hàm lưu hoặc cập nhật gallery
    private void saveGallery() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String type = spinnerType.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (type.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn loại bộ sưu tập", Toast.LENGTH_SHORT).show();
            return;
        }

        // Chuẩn bị đối tượng để lưu
        Gallery galleryToSave = (currentGalleryId == null) ? new Gallery() : galleryToEdit;
        galleryToSave.setName(name);
        galleryToSave.setDescription(description);
        galleryToSave.setType(type);

        // Gán ID mới nếu là thêm
        if (currentGalleryId == null) {
            galleryToSave.setId(UUID.randomUUID().toString());
        }

        executeSaveOrUpdate(galleryToSave);
    }

    // Thực thi lưu hoặc cập nhật
    private void executeSaveOrUpdate(Gallery gallery) {
        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditGalleryActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu gallery: ", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditGalleryActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        };

        if (currentGalleryId == null) {
            galleryService.insertGallery(gallery, callback);
        } else {
            galleryService.updateGallery(currentGalleryId, gallery, callback);
        }
    }
}
