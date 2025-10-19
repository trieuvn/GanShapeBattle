package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.SupabaseCallback;

public class GalleryDetailActivity extends AppCompatActivity {

    private TextView tvId, tvName, tvDescription, tvType;
    private Button btnUpdate, btnDelete;
    private GalleryService galleryService;
    private String currentGalleryId;
    private ActivityResultLauncher<Intent> editGalleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_detail);

        tvId = findViewById(R.id.tvGalleryId);
        tvName = findViewById(R.id.tvGalleryName);
        tvDescription = findViewById(R.id.tvGalleryDescription);
        tvType = findViewById(R.id.tvGalleryType);
        btnUpdate = findViewById(R.id.btnUpdateGallery);
        btnDelete = findViewById(R.id.btnDeleteGallery);

        galleryService = new GalleryService();
        currentGalleryId = getIntent().getStringExtra("GALLERY_ID");

        if (currentGalleryId != null) {
            fetchGalleryDetails(currentGalleryId);
        }

        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditGalleryActivity.class);
            intent.putExtra("GALLERY_ID_EDIT", currentGalleryId);
            editGalleryLauncher.launch(intent);
        });

        btnDelete.setOnClickListener(v -> deleteGallery(currentGalleryId));

        editGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchGalleryDetails(currentGalleryId);
                    }
                }
        );
    }

    private void fetchGalleryDetails(String galleryId) {
        galleryService.getGalleryById(galleryId, new SupabaseCallback<Gallery>() {
            @Override
            public void onSuccess(Gallery gallery) {
                runOnUiThread(() -> {
                    if (gallery != null) {
                        tvId.setText(gallery.getId());
                        tvName.setText(gallery.getName());
                        tvDescription.setText(gallery.getDescription());
                        tvType.setText(gallery.getType());
                    }
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }

    private void deleteGallery(String galleryId) {
        galleryService.deleteGallery(galleryId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(GalleryDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }
}