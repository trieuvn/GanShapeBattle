package com.ganshapebattle.admin; // Hoặc package com.ganshapebattle tùy thuộc vào vị trí tệp

import android.content.Intent;
import android.graphics.Bitmap; // Import Bitmap
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
import com.ganshapebattle.R;
// Import ImageUtils
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.SupabaseCallback;

public class PictureDetailActivity extends AppCompatActivity {

    private static final String TAG = "PictureDetailActivity";

    private ImageView ivPictureImage;
    private TextView tvPictureName, tvPictureDescription, tvPictureCreatedDate, tvPictureType, tvPictureTags; // Thêm tvPictureTags nếu có trong layout
    private Button btnUpdatePicture, btnDeletePicture;

    private PictureService pictureService;
    private String currentPictureId;

    private ActivityResultLauncher<Intent> editPictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_detail);

        // Ánh xạ View (đảm bảo ID khớp với layout của bạn)
        ivPictureImage = findViewById(R.id.ivPictureImage);
        tvPictureName = findViewById(R.id.tvPictureName);
        tvPictureDescription = findViewById(R.id.tvPictureDescription);
        tvPictureCreatedDate = findViewById(R.id.tvPictureCreatedDate);
        tvPictureType = findViewById(R.id.tvPictureType);
        // tvPictureTags = findViewById(R.id.tvPictureTags); // Bỏ comment nếu có TextView cho Tags
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

        btnDeletePicture.setOnClickListener(v -> {
            // Thêm hộp thoại xác nhận trước khi xóa (nên làm)
            deletePicture(currentPictureId);
        });

        editPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchPictureDetails(currentPictureId); // Tải lại nếu có thay đổi
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
                        tvPictureName.setText(picture.getName() != null ? picture.getName() : "N/A");
                        tvPictureDescription.setText(picture.getDescription() != null ? picture.getDescription() : "N/A");
                        tvPictureCreatedDate.setText(picture.getCreatedDate() != null ? picture.getCreatedDate() : "N/A");
                        tvPictureType.setText(picture.getType() != null ? picture.getType() : "N/A");
                        // if (tvPictureTags != null) { // Hiển thị tags nếu có TextView
                        //     tvPictureTags.setText(picture.getTags() != null ? picture.getTags() : "N/A");
                        // }

                        // --- CẬP NHẬT LOGIC HIỂN THỊ HÌNH ẢNH ---
                        String imageData = picture.getImage();
                        if (imageData != null && !imageData.isEmpty()) {
                            if (imageData.startsWith("http")) {
                                // Dữ liệu cũ là URL, dùng Glide
                                Glide.with(PictureDetailActivity.this)
                                        .load(imageData)
                                        .placeholder(R.drawable.ic_default_avatar) // Ảnh chờ
                                        .error(R.drawable.ic_default_avatar) // Ảnh lỗi
                                        .into(ivPictureImage);
                            } else {
                                // Dữ liệu mới là Base64, giải mã
                                Bitmap imgBitmap = ImageUtils.base64ToBitmap(imageData);
                                if (imgBitmap != null) {
                                    ivPictureImage.setImageBitmap(imgBitmap); // Hiển thị Bitmap
                                } else {
                                    // Lỗi giải mã Base64
                                    Log.w(TAG, "Lỗi giải mã Base64 image cho picture ID: " + pictureId);
                                    ivPictureImage.setImageResource(R.drawable.ic_default_avatar); // Ảnh mặc định
                                }
                            }
                        } else {
                            // Không có dữ liệu ảnh
                            ivPictureImage.setImageResource(R.drawable.ic_default_avatar); // Ảnh mặc định
                        }
                        // --- KẾT THÚC CẬP NHẬT ---

                    } else {
                        Toast.makeText(PictureDetailActivity.this, "Không tìm thấy thông tin ảnh", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching picture details: ", e);
                runOnUiThread(() -> Toast.makeText(PictureDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void deletePicture(String pictureId) {
        pictureService.deletePicture(pictureId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(PictureDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo hiệu xóa thành công
                    finish();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error deleting picture: ", e);
                runOnUiThread(() -> Toast.makeText(PictureDetailActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}