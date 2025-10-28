package com.ganshapebattle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class LobbyActivity extends AppCompatActivity {

    // --- Static method ---
    public static Intent createIntent(android.content.Context context, String username) {
        Intent intent = new Intent(context, LobbyActivity.class);
        intent.putExtra("username", username);
        return intent;
    }

    // --- UI Elements ---
    private MaterialButton buttonCreateLobby, buttonBegin, buttonDelete;
    private ImageView imageViewQRCode;
    private TextView textViewLobbyId;
    private LinearLayout layoutLobbyInfo, layoutControls;
    private MaterialButtonToggleGroup toggleGroupMode;

    // --- Services & Data ---
    private LobbyService lobbyService;
    private PlayerService playerService;
    private UserService userService;
    private Lobby currentLobby;
    private User currentUser;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Lấy username từ Intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin người dùng!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Khởi tạo services
        lobbyService = new LobbyService();
        playerService = new PlayerService();
        userService = new UserService();

        // Ánh xạ View
        bindViews();
        setupListeners();

        // Tải thông tin người dùng
        loadUserInfo();
    }

    /** Truy vấn thông tin user */
    private void loadUserInfo() {
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        currentUser = user;
                        Toast.makeText(LobbyActivity.this, "Chào mừng " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LobbyActivity.this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyActivity.this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    /** Ánh xạ các thành phần từ layout */
    private void bindViews() {
        buttonCreateLobby = findViewById(R.id.buttonCreateLobby);
        buttonBegin = findViewById(R.id.buttonBegin);
        buttonDelete = findViewById(R.id.buttonDelete);
        imageViewQRCode = findViewById(R.id.imageViewQRCode);
        textViewLobbyId = findViewById(R.id.textViewLobbyId);
        layoutLobbyInfo = findViewById(R.id.layoutLobbyInfo);
        layoutControls = findViewById(R.id.layoutControls);
        toggleGroupMode = findViewById(R.id.toggleGroupMode);
    }

    /** Lắng nghe sự kiện */
    private void setupListeners() {
        buttonCreateLobby.setOnClickListener(v -> createLobby());
        buttonBegin.setOnClickListener(v -> beginGame());
        buttonDelete.setOnClickListener(v -> deleteLobby());

        // Lắng nghe toggle giữa Vote / Rate
        toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && currentLobby != null) {
                String newMode = (checkedId == R.id.btnVote) ? "vote" : "rate";
                setMode(newMode);
            }
        });
    }

    /** Tạo phòng và hiển thị mã QR */
    private void createLobby() {
        if (currentUser == null) {
            Toast.makeText(this, "Chưa tải được thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date());
            String lobbyId = UUID.randomUUID().toString();

            currentLobby = new Lobby();
            currentLobby.setId(lobbyId);
            currentLobby.setAdminUsername(currentUser.getUsername());
            currentLobby.setMode("vote");
            currentLobby.setStatus("waiting");
            currentLobby.setMaxPlayer(10);
            currentLobby.setDesignTime(300);
            currentLobby.setVoteTime(60);
            currentLobby.setCreatedDate(currentTime);
            currentLobby.setBeginDate(null);

            lobbyService.insertLobby(currentLobby, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyActivity.this, "Tạo phòng thành công!", Toast.LENGTH_SHORT).show();
                        createQrcode(currentLobby.getId());
                        updateUIAfterCreation();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(LobbyActivity.this, "Lỗi tạo phòng: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Cập nhật chế độ chơi */
    private void setMode(String mode) {
        currentLobby.setMode(mode);
        lobbyService.updateLobby(currentLobby.getId(), currentLobby, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() ->
                        Toast.makeText(LobbyActivity.this, "Đã đổi chế độ: " + mode, Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(LobbyActivity.this, "Lỗi cập nhật chế độ: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    /** Bắt đầu trò chơi */
    private void beginGame() {
        if (currentLobby == null) {
            Toast.makeText(this, "Chưa có phòng chơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String beginDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date());
            currentLobby.setStatus("in_progress");
            currentLobby.setBeginDate(beginDate);

            lobbyService.updateLobby(currentLobby.getId(), currentLobby, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyActivity.this, "Trò chơi bắt đầu!", Toast.LENGTH_SHORT).show();
                        // TODO: start GameActivity
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(LobbyActivity.this, "Lỗi bắt đầu trò chơi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi bắt đầu trò chơi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** Xóa phòng */
    private void deleteLobby() {
        if (currentLobby == null) {
            Toast.makeText(this, "Chưa có phòng chơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        deleteLobbyFromDatabase(currentLobby.getId());
    }

    private void deleteLobbyFromDatabase(String lobbyId) {
        lobbyService.deleteLobby(lobbyId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyActivity.this, "Đã xóa phòng.", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(LobbyActivity.this, "Lỗi xóa phòng: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    /** Tạo QR từ lobby ID */
    private void createQrcode(String lobbyId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(lobbyId, BarcodeFormat.QR_CODE, 400, 400);
            imageViewQRCode.setImageBitmap(bitmap);
            textViewLobbyId.setText("Mã phòng: " + lobbyId);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    /** Cập nhật giao diện sau khi tạo phòng */
    private void updateUIAfterCreation() {
        buttonCreateLobby.setVisibility(View.GONE);
        layoutLobbyInfo.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);
    }

    /** Đưa giao diện về trạng thái ban đầu */
    private void resetUI() {
        currentLobby = null;
        layoutLobbyInfo.setVisibility(View.GONE);
        layoutControls.setVisibility(View.GONE);
        buttonCreateLobby.setVisibility(View.VISIBLE);
        imageViewQRCode.setImageBitmap(null);
        textViewLobbyId.setText("");
    }
}
