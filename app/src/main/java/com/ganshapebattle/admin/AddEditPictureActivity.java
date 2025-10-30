package com.ganshapebattle.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.ganshapebattle.R;
import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

public class AddEditPictureActivity extends AppCompatActivity {

    private static final String TAG = "AddEditPictureActivity";

    // Views
    private TextView tvTitle;
    private ImageView ivSelectedImage;
    private MaterialButton btnSelectImage, btnSavePicture;
    private ExtendedFloatingActionButton fabSave;
    private EditText etName, etDescription;
    private RadioButton rbTypeArt, rbTypePhoto, rbTypeDigital;
    private ChipGroup chipGroupTags;
    private AutoCompleteTextView spinnerGallery, spinnerUser;

    // Services
    private PictureService pictureService;
    private GalleryService galleryService;
    private UserService userService;

    // Data
    private List<Gallery> galleryList = new ArrayList<>();
    private List<User> userList = new ArrayList<>();
    private String currentPictureId = null;
    private Picture pictureToEdit;
    private Uri selectedImageUri;

    // Launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_picture);

        setupViews();
        setupLaunchers();

        pictureService = new PictureService();
        galleryService = new GalleryService();
        userService = new UserService();

        loadGalleries();
        loadUsers();

        currentPictureId = getIntent().getStringExtra("PICTURE_ID_EDIT");
        if (currentPictureId != null) {
            tvTitle.setText("Chỉnh sửa hình ảnh");
            loadPictureDetails(currentPictureId);
        } else {
            tvTitle.setText("Thêm hình ảnh mới");
        }

        btnSelectImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSavePicture.setOnClickListener(v -> savePicture());
        fabSave.setOnClickListener(v -> savePicture());
    }

    private void setupViews() {
        tvTitle = findViewById(R.id.tvPictureTitle);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSavePicture = findViewById(R.id.btnSavePicture);
        //fabSave = findViewById(R.id.fabSave);

        etName = findViewById(R.id.etPictureName);
        etDescription = findViewById(R.id.etPictureDescription);

        rbTypeArt = findViewById(R.id.rbTypeArt);
        rbTypePhoto = findViewById(R.id.rbTypePhoto);
        rbTypeDigital = findViewById(R.id.rbTypeDigital);

        chipGroupTags = findViewById(R.id.checkboxGroupTags);

        spinnerGallery = findViewById(R.id.spinnerPictureGallery);
        spinnerUser = findViewById(R.id.spinnerPictureUser);
    }

    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openImagePicker();
                    else Toast.makeText(this, "Cần quyền truy cập để chọn ảnh", Toast.LENGTH_SHORT).show();
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                            ivSelectedImage.setImageBitmap(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void loadGalleries() {
        galleryService.getAllGalleries(new SupabaseCallback<List<Gallery>>() {
            @Override
            public void onSuccess(List<Gallery> galleries) {
                galleryList = galleries;
                List<String> galleryNames = galleryList.stream().map(Gallery::getName).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPictureActivity.this, android.R.layout.simple_dropdown_item_1line, galleryNames);
                    spinnerGallery.setAdapter(adapter);
                    if (pictureToEdit != null) setGallerySelection();
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải galleries", e);
            }
        });
    }

    private void loadUsers() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList = users;
                List<String> usernames = userList.stream().map(User::getUsername).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPictureActivity.this, android.R.layout.simple_dropdown_item_1line, usernames);
                    spinnerUser.setAdapter(adapter);
                    if (pictureToEdit != null)
                        spinnerUser.setText(pictureToEdit.getUsername(), false);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải users", e);
            }
        });
    }

    private void loadPictureDetails(String id) {
        pictureService.getPictureById(id, new SupabaseCallback<Picture>() {
            @Override
            public void onSuccess(Picture picture) {
                pictureToEdit = picture;
                runOnUiThread(() -> {
                    etName.setText(picture.getName());
                    etDescription.setText(picture.getDescription());
                    Glide.with(AddEditPictureActivity.this).load(picture.getImage()).into(ivSelectedImage);

                    if (picture.getType().equalsIgnoreCase("Nghệ thuật")) rbTypeArt.setChecked(true);
                    else if (picture.getType().equalsIgnoreCase("Nhiếp ảnh")) rbTypePhoto.setChecked(true);
                    else rbTypeDigital.setChecked(true);

                    if (picture.getTags() != null) {
                        List<String> tags = Arrays.asList(picture.getTags().split(",\\s*"));
                        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
                            Chip chip = (Chip) chipGroupTags.getChildAt(i);
                            chip.setChecked(tags.contains(chip.getText().toString()));
                        }
                    }

                    setGallerySelection();
                    spinnerUser.setText(picture.getUsername(), false);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải chi tiết ảnh", e);
            }
        });
    }

    private void setGallerySelection() {
        if (pictureToEdit == null || galleryList.isEmpty()) return;
        for (Gallery g : galleryList) {
            if (g.getId().equals(pictureToEdit.getGalleryId())) {
                spinnerGallery.setText(g.getName(), false);
                return;
            }
        }
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void savePicture() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Tên ảnh không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedType = rbTypeArt.isChecked() ? "Nghệ thuật"
                : rbTypePhoto.isChecked() ? "Nhiếp ảnh" : "Digital Art";

        StringJoiner tagsJoiner = new StringJoiner(", ");
        for (int i = 0; i < chipGroupTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupTags.getChildAt(i);
            if (chip.isChecked()) tagsJoiner.add(chip.getText().toString());
        }

        String galleryName = spinnerGallery.getText().toString();
        String username = spinnerUser.getText().toString();

        if (galleryName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn bộ sưu tập và người tạo", Toast.LENGTH_SHORT).show();
            return;
        }

        Gallery selectedGallery = galleryList.stream()
                .filter(g -> g.getName().equalsIgnoreCase(galleryName))
                .findFirst().orElse(null);

        if (selectedGallery == null) {
            Toast.makeText(this, "Không tìm thấy gallery", Toast.LENGTH_SHORT).show();
            return;
        }

        Picture picture = (currentPictureId == null) ? new Picture() : pictureToEdit;
        picture.setName(name);
        picture.setDescription(etDescription.getText().toString());
        picture.setType(selectedType);
        picture.setTags(tagsJoiner.toString());
        picture.setGalleryId(selectedGallery.getId());
        picture.setUsername(username);

        if (currentPictureId == null) {
            picture.setId(UUID.randomUUID().toString());
            picture.setCreatedDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date()));
        }

        if (selectedImageUri != null) {
            Bitmap bitmap = ((BitmapDrawable) ivSelectedImage.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            String fileName = "pic_" + System.currentTimeMillis() + ".jpg";
            pictureService.uploadPictureImage(fileName, data, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String url) {
                    picture.setImage(url);
                    saveOrUpdate(picture);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AddEditPictureActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveOrUpdate(picture);
        }
    }

    private void saveOrUpdate(Picture picture) {
        if (currentPictureId == null) {
            pictureService.insertPicture(picture, new SupabaseCallback<Picture>() {
                @Override
                public void onSuccess(Picture p) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditPictureActivity.this, "Thêm thành công!", Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AddEditPictureActivity.this, "Thêm thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            pictureService.updatePicture(currentPictureId, picture, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddEditPictureActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(AddEditPictureActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
