package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.SupabaseCallback;

public class PictureDetailActivity extends AppCompatActivity {

    private static final String TAG = "PictureDetailActivity";

    private ImageView ivPictureImage;
    private TextView tvPictureName, tvPictureDescription, tvPictureCreatedDate, tvPictureType;
    private Button btnUpdatePicture, btnDeletePicture;

    private PictureService pictureService;
    private String currentPictureId;

    private ActivityResultLauncher<Intent> editPictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_detail);

        ivPictureImage = findViewById(R.id.ivPictureImage);
        tvPictureName = findViewById(R.id.tvPictureName);
        tvPictureDescription = findViewById(R.id.tvPictureDescription);
        tvPictureCreatedDate = findViewById(R.id.tvPictureCreatedDate);
        tvPictureType = findViewById(R.id.tvPictureType);
        btnUpdatePicture = findViewById(R.id.btnUpdatePicture);
        btnDeletePicture = findViewById(R.id.btnDeletePicture);

        pictureService = new PictureService();
        currentPictureId = getIntent().getStringExtra("PICTURE_ID");

        if (currentPictureId != null && !currentPictureId.isEmpty()) {
            fetchPictureDetails(currentPictureId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID hình ảnh", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnUpdatePicture.setOnClickListener(v -> {
            Intent intent = new Intent(PictureDetailActivity.this, AddEditPictureActivity.class);
            intent.putExtra("PICTURE_ID_EDIT", currentPictureId);
            editPictureLauncher.launch(intent);
        });

        btnDeletePicture.setOnClickListener(v -> deletePicture(currentPictureId));

        editPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchPictureDetails(currentPictureId);
                    }
                }
        );
    }

    private void fetchPictureDetails(String pictureId) {
        pictureService.getPictureById(pictureId, new SupabaseCallback<Picture>() {
            @Override
            public void onSuccess(Picture picture) {
                runOnUiThread(() -> {
                    if (picture != null) {
                        tvPictureName.setText(picture.getName());
                        tvPictureDescription.setText(picture.getDescription());
                        tvPictureCreatedDate.setText(picture.getCreatedDate());
                        tvPictureType.setText(picture.getType());

                        Glide.with(PictureDetailActivity.this)
                                .load(picture.getImage())
                                .placeholder(R.drawable.ic_default_avatar)
                                .into(ivPictureImage);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching picture details: ", e);
            }
        });
    }

    private void deletePicture(String pictureId) {
        pictureService.deletePicture(pictureId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(PictureDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error deleting picture: ", e);
            }
        });
    }
}