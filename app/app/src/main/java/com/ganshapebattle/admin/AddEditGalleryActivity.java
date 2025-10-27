package com.ganshapebattle.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.UUID;

public class AddEditGalleryActivity extends AppCompatActivity {

    private TextView tvTitle;
    private EditText etName, etDescription;
    private Spinner spinnerType;
    private Button btnSave;

    private GalleryService galleryService;
    private String currentGalleryId = null;
    private Gallery galleryToEdit;

    private final String[] galleryTypes = {"Public", "Private", "Shared"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_gallery);

        tvTitle = findViewById(R.id.tvGalleryTitle);
        etName = findViewById(R.id.etGalleryName);
        etDescription = findViewById(R.id.etGalleryDescription);
        spinnerType = findViewById(R.id.spinnerGalleryType);
        btnSave = findViewById(R.id.btnSaveGallery);

        setupSpinner();
        galleryService = new GalleryService();
        currentGalleryId = getIntent().getStringExtra("GALLERY_ID_EDIT");

        if (currentGalleryId != null) {
            tvTitle.setText("Chỉnh sửa bộ sưu tập");
            loadGalleryDetails(currentGalleryId);
        } else {
            tvTitle.setText("Tạo bộ sưu tập mới");
        }

        btnSave.setOnClickListener(v -> saveGallery());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, galleryTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
    }

    private void loadGalleryDetails(String galleryId) {
        galleryService.getGalleryById(galleryId, new SupabaseCallback<Gallery>() {
            @Override
            public void onSuccess(Gallery gallery) {
                if (gallery != null) {
                    galleryToEdit = gallery;
                    runOnUiThread(() -> {
                        etName.setText(gallery.getName());
                        etDescription.setText(gallery.getDescription());
                        setSpinnerSelection(spinnerType, galleryTypes, gallery.getType());
                    });
                }
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void saveGallery() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        Gallery galleryToSave = (currentGalleryId == null) ? new Gallery() : galleryToEdit;
        galleryToSave.setName(name);
        galleryToSave.setDescription(etDescription.getText().toString().trim());
        galleryToSave.setType(spinnerType.getSelectedItem().toString());

        // ID được tạo tự động bởi Supabase, không cần gán ở đây khi insert
        // Chỉ cần khi update
        if (currentGalleryId != null) {
            galleryToSave.setId(currentGalleryId);

        } else {
            galleryToSave.setId(UUID.randomUUID().toString());
        }

        executeSaveOrUpdate(galleryToSave);
    }

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
                Log.e("AddEditGallery", "Lỗi khi lưu: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditGalleryActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        };

        if (currentGalleryId == null) {
            galleryService.insertGallery(gallery, callback);
        } else {
            galleryService.updateGallery(currentGalleryId, gallery, callback);
        }
    }
}