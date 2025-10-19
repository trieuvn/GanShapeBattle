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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private Button btnSelectImage, btnSavePicture;
    private EditText etName, etDescription;
    private RadioGroup radioGroupType;
    private List<CheckBox> tagCheckBoxes;
    private Spinner spinnerGallery, spinnerUser;

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

    // Activity Launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_picture);

        // Ánh xạ views
        setupViews();

        // Khởi tạo services
        pictureService = new PictureService();
        galleryService = new GalleryService();
        userService = new UserService();

        // Tải dữ liệu cho Spinners
        loadGalleriesIntoSpinner();
        loadUsersIntoSpinner();

        // Kiểm tra chế độ Thêm/Sửa
        currentPictureId = getIntent().getStringExtra("PICTURE_ID_EDIT");
        if (currentPictureId != null) {
            tvTitle.setText("Chỉnh sửa hình ảnh");
            loadPictureDetailsForEdit(currentPictureId);
        } else {
            tvTitle.setText("Thêm hình ảnh mới");
        }

        // Sự kiện
        btnSelectImage.setOnClickListener(v -> checkPermissionAndPickImage());
        btnSavePicture.setOnClickListener(v -> savePicture());

        // Khởi tạo launchers
        setupLaunchers();
    }

    private void setupViews() {
        tvTitle = findViewById(R.id.tvPictureTitle);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSavePicture = findViewById(R.id.btnSavePicture);
        etName = findViewById(R.id.etPictureName);
        etDescription = findViewById(R.id.etPictureDescription);
        radioGroupType = findViewById(R.id.radioGroupType);
        spinnerGallery = findViewById(R.id.spinnerPictureGallery);
        spinnerUser = findViewById(R.id.spinnerPictureUser);

        tagCheckBoxes = new ArrayList<>();
        tagCheckBoxes.add(findViewById(R.id.cbTagNature));
        tagCheckBoxes.add(findViewById(R.id.cbTagCity));
        tagCheckBoxes.add(findViewById(R.id.cbTagPeople));
    }

    private void loadGalleriesIntoSpinner() {
        galleryService.getAllGalleries(new SupabaseCallback<List<Gallery>>() {
            @Override
            public void onSuccess(List<Gallery> galleries) {
                galleryList = galleries;
                List<String> galleryNames = galleryList.stream().map(Gallery::getName).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPictureActivity.this, android.R.layout.simple_spinner_item, galleryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerGallery.setAdapter(adapter);
                    if (pictureToEdit != null) {
                        setGallerySpinnerSelection();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải galleries: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditPictureActivity.this, "Không thể tải danh sách Gallery", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadUsersIntoSpinner() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList = users;
                List<String> usernames = userList.stream().map(User::getUsername).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPictureActivity.this, android.R.layout.simple_spinner_item, usernames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUser.setAdapter(adapter);
                    if (pictureToEdit != null) {
                        setSpinnerSelection(spinnerUser, usernames, pictureToEdit.getUsername());
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải users: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditPictureActivity.this, "Không thể tải danh sách User", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadPictureDetailsForEdit(String pictureId) {
        pictureService.getPictureById(pictureId, new SupabaseCallback<Picture>() {
            @Override
            public void onSuccess(Picture picture) {
                if (picture != null) {
                    pictureToEdit = picture;
                    runOnUiThread(() -> {
                        etName.setText(pictureToEdit.getName());
                        etDescription.setText(pictureToEdit.getDescription());
                        Glide.with(AddEditPictureActivity.this).load(pictureToEdit.getImage()).into(ivSelectedImage);

                        // Cài đặt giá trị cho RadioGroup
                        for (int i = 0; i < radioGroupType.getChildCount(); i++) {
                            RadioButton radioButton = (RadioButton) radioGroupType.getChildAt(i);
                            if (radioButton.getText().toString().equalsIgnoreCase(pictureToEdit.getType())) {
                                radioButton.setChecked(true);
                                break;
                            }
                        }

                        // Cài đặt giá trị cho CheckBoxes
                        if (pictureToEdit.getTags() != null && !pictureToEdit.getTags().isEmpty()) {
                            List<String> tags = Arrays.asList(pictureToEdit.getTags().split(",\\s*"));
                            for (CheckBox checkBox : tagCheckBoxes) {
                                checkBox.setChecked(tags.contains(checkBox.getText().toString()));
                            }
                        }

                        // Tải lại dữ liệu spinner để đảm bảo chọn đúng item
                        loadGalleriesIntoSpinner();
                        loadUsersIntoSpinner();
                    });
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải chi tiết picture: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditPictureActivity.this, "Lỗi tải chi tiết hình ảnh", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void savePicture() {
        if (spinnerGallery.getSelectedItem() == null || spinnerUser.getSelectedItem() == null) {
            Toast.makeText(this, "Vui lòng chọn Gallery và User", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Tên ảnh không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        final Picture pictureToSave = (currentPictureId == null) ? new Picture() : pictureToEdit;

        // Lấy giá trị từ RadioGroup
        int selectedTypeId = radioGroupType.getCheckedRadioButtonId();
        if (selectedTypeId == -1) {
            Toast.makeText(this, "Vui lòng chọn Loại", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = findViewById(selectedTypeId);
        String selectedType = selectedRadioButton.getText().toString();

        // Lấy giá trị từ CheckBoxes
        StringJoiner tagJoiner = new StringJoiner(", ");
        for (CheckBox checkBox : tagCheckBoxes) {
            if (checkBox.isChecked()) {
                tagJoiner.add(checkBox.getText().toString());
            }
        }
        String selectedTags = tagJoiner.toString();

        // Lấy ID và username từ các Spinner
        String selectedGalleryId = galleryList.get(spinnerGallery.getSelectedItemPosition()).getId();
        String selectedUsername = userList.get(spinnerUser.getSelectedItemPosition()).getUsername();

        // Gán các giá trị vào đối tượng Picture
        pictureToSave.setName(name);
        pictureToSave.setDescription(etDescription.getText().toString().trim());
        pictureToSave.setType(selectedType);
        pictureToSave.setTags(selectedTags);
        pictureToSave.setGalleryId(selectedGalleryId);
        pictureToSave.setUsername(selectedUsername);

        // Gán các giá trị mặc định khi tạo mới
        if (currentPictureId == null) {
            pictureToSave.setId(UUID.randomUUID().toString());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            pictureToSave.setCreatedDate(sdf.format(new Date()));
        }

        // Xử lý upload ảnh nếu có hoặc lưu trực tiếp
        if (selectedImageUri != null) {
            Bitmap bitmap = ((BitmapDrawable) ivSelectedImage.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();
            String fileName = "pic_" + System.currentTimeMillis() + ".jpg";

            pictureService.uploadPictureImage(fileName, imageData, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String imageUrl) {
                    pictureToSave.setImage(imageUrl);
                    executeSaveOrUpdate(pictureToSave);
                }
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Lỗi upload ảnh: ", e);
                    runOnUiThread(() -> Toast.makeText(AddEditPictureActivity.this, "Lỗi upload ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            if (currentPictureId != null && pictureToEdit.getImage() != null) {
                executeSaveOrUpdate(pictureToSave);
            } else {
                Toast.makeText(this, "Vui lòng chọn một hình ảnh khi tạo mới", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void executeSaveOrUpdate(Picture picture) {
        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditPictureActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu ảnh: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditPictureActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        };

        if (currentPictureId == null) {
            pictureService.insertPicture(picture, callback);
        } else {
            pictureService.updatePicture(currentPictureId, picture, callback);
        }
    }

    private void setupLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Cần quyền truy cập để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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

    private void setSpinnerSelection(Spinner spinner, List<String> options, String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setGallerySpinnerSelection() {
        if (pictureToEdit == null || galleryList.isEmpty()) return;

        for (int i = 0; i < galleryList.size(); i++) {
            if (galleryList.get(i).getId().equals(pictureToEdit.getGalleryId())) {
                spinnerGallery.setSelection(i);
                return;
            }
        }
    }

    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }
}