package com.ganshapebattle.admin;

import android.graphics.Bitmap; // <-- Thêm
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView; // <-- Thêm
import android.widget.Spinner;
import android.widget.TextView;
//import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;
import com.ganshapebattle.utils.ImageUtils; // <-- Thêm

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddEditPlayerActivity extends AppCompatActivity {

    private static final String TAG = "AddEditPlayerActivity";

    private TextView tvTitle;
    private Spinner spinnerUser, spinnerLobby;
    private ImageView imageViewPlayerPicture; // <-- Sửa: Từ Spinner sang ImageView
    private EditText etPoint;
    private Button btnSave;

    private PlayerService playerService;
    private UserService userService;
    private LobbyService lobbyService;
    private PictureService pictureService; // Cần giữ lại để dùng trong getPicture()

    private List<User> userList = new ArrayList<>();
    private List<Lobby> lobbyList = new ArrayList<>();
    // private List<Picture> pictureList = new ArrayList<>(); // Không cần tải tất cả picture nữa
    private List<String> userNames = new ArrayList<>();
    private List<String> lobbyIds = new ArrayList<>();
    // private List<String> pictureInfos = new ArrayList<>(); // Không cần

    private Player playerToEdit;
    private String currentUsername;
    private String currentLobbyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_player);

        bindViews();
        initServices();

        btnSave.setOnClickListener(v -> savePlayer());

        // Lấy dữ liệu từ Intent (do LobbyRateVoteActivity gửi)
        currentUsername = getIntent().getStringExtra("PLAYER_USERNAME");
        currentLobbyId = getIntent().getStringExtra("LOBBY_ID");

        // --- SỬA LỖI: Xóa tham chiếu đến spinnerPicture ---
        if (spinnerUser != null) spinnerUser.setEnabled(false);
        if (spinnerLobby != null) spinnerLobby.setEnabled(false);
        // Đã xóa spinnerPicture.setEnabled(false)

        if (currentUsername != null && currentLobbyId != null) {
            tvTitle.setText("Chấm điểm Player");
            loadDataForEditMode();
        } else {
            tvTitle.setText("Thêm Player Mới");
            loadDataForCreateMode();
        }
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvPlayerTitle);
        spinnerUser = findViewById(R.id.spinnerUser);
        spinnerLobby = findViewById(R.id.spinnerLobby);
        // Sửa: Ánh xạ ID mới từ XML
        imageViewPlayerPicture = findViewById(R.id.imageViewPlayerPicture);
        etPoint = findViewById(R.id.etPlayerPoint);
        btnSave = findViewById(R.id.btnSavePlayer);
    }

    private void initServices() {
        playerService = new PlayerService();
        userService = new UserService();
        lobbyService = new LobbyService();
        pictureService = new PictureService(); // Vẫn cần
    }

    private void loadDataForEditMode() {
        // Tải 2 danh sách (User, Lobby) và Player cần sửa
        loadUsers();
        loadLobbies();
        // loadPictures(); // Xóa: Không cần tải tất cả
        loadPlayerToEdit();
    }

    private void loadDataForCreateMode() {
        // Tải 2 danh sách (User, Lobby)
        loadUsers();
        loadLobbies();
        // loadPictures(); // Xóa
        // Bật spinner
        if (spinnerUser != null) spinnerUser.setEnabled(true);
        if (spinnerLobby != null) spinnerLobby.setEnabled(true);
        // Xóa spinnerPicture.setEnabled(true)
    }

    private void loadPlayerToEdit() {
        playerService.getPlayerByIds(currentUsername, currentLobbyId, new SupabaseCallback<Player>() {
            @Override
            public void onSuccess(Player player) {
                playerToEdit = player;
                if (player == null) {
//                    runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Không tìm thấy Player", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Cập nhật UI (Tên, Lobby, Điểm)
                runOnUiThread(() -> {
                    if (player != null) {
                        etPoint.setText(String.valueOf(player.getPoint()));
                        setSpinnerSelection(spinnerUser, userNames, player.getUsername());
                        setSpinnerSelection(spinnerLobby, lobbyIds, player.getLobbyId());
                    }
                });

                // --- LOGIC MỚI: Tải ảnh của Player ---
                Log.d(TAG, "Đang tải Picture cho Player: " + player.getUsername());
                player.getPicture(new SupabaseCallback<Picture>() {
                    @Override
                    public void onSuccess(Picture picture) {
                        if (picture == null || picture.getImage() == null || picture.getImage().isEmpty()) {
//                            runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Không tìm thấy dữ liệu ảnh", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // 1. Lấy chuỗi base64
                        String base64String = picture.getImage();

                        // 2. Decode (sử dụng ImageUtils)
                        // (Lưu ý: Nếu ảnh quá lớn, nên chạy cái này trong AsyncTask/thread)
                        Bitmap imageBitmap = ImageUtils.base64ToBitmap(base64String);

                        // 3. Hiển thị (trên UI Thread)
                        runOnUiThread(() -> {
                            if (imageBitmap != null) {
                                imageViewPlayerPicture.setImageBitmap(imageBitmap);
                            } else {
//                                Toast.makeText(AddEditPlayerActivity.this, "Lỗi giải mã ảnh base64", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
//                        runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi tải Picture: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
                // --- KẾT THÚC LOGIC MỚI ---
            }
            @Override
            public void onFailure(Exception e) {
//                runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi tải Player: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadUsers() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList = users;
                userNames = userList.stream().map(User::getUsername).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> userAdapter = new ArrayAdapter<>(AddEditPlayerActivity.this, android.R.layout.simple_spinner_item, userNames);
                    userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    if (spinnerUser != null) spinnerUser.setAdapter(userAdapter);
                    // Chọn lại nếu là edit mode
                    if (playerToEdit != null) setSpinnerSelection(spinnerUser, userNames, playerToEdit.getUsername());
                });
            }
            @Override
            public void onFailure(Exception e) {
//                runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi tải Users", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadLobbies() {
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> lobbies) {
                lobbyList = lobbies;
                lobbyIds = lobbyList.stream().map(Lobby::getId).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> lobbyAdapter = new ArrayAdapter<>(AddEditPlayerActivity.this, android.R.layout.simple_spinner_item, lobbyIds);
                    lobbyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    if (spinnerLobby != null) spinnerLobby.setAdapter(lobbyAdapter);
                    if (playerToEdit != null) setSpinnerSelection(spinnerLobby, lobbyIds, playerToEdit.getLobbyId());
                });
            }
            @Override
            public void onFailure(Exception e) {
//                runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi tải Lobbies", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Xóa: private void loadPictures() { ... }

    private void savePlayer() {
        if (etPoint.getText().toString().isEmpty()) {
//            Toast.makeText(this, "Vui lòng nhập điểm", Toast.LENGTH_SHORT).show();
            return;
        }

        Player player = (playerToEdit != null) ? playerToEdit : new Player();

        try {
            player.setPoint(Integer.parseInt(etPoint.getText().toString()));
        } catch (NumberFormatException e) {
//            Toast.makeText(this, "Điểm không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (playerToEdit == null) { // Chế độ Create (Đã bị đơn giản hóa)
            if (spinnerUser == null || spinnerLobby == null) { // Xóa check spinnerPicture
//                Toast.makeText(this, "Lỗi Spinner bị null", Toast.LENGTH_SHORT).show();
                return;
            }
            player.setUsername(userList.get(spinnerUser.getSelectedItemPosition()).getUsername());
            player.setLobbyId(lobbyList.get(spinnerLobby.getSelectedItemPosition()).getId());
            // player.setPictureId(pictureList.get(spinnerPicture.getSelectedItemPosition()).getId()); // Xóa dòng này
            player.setPictureId(null); // Hoặc bạn cần logic khác để gán Picture ID
            player.setRank(0);
        }
        // Nếu là edit mode (đang chấm điểm), chúng ta chỉ thay đổi điểm (point)

        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
//                    Toast.makeText(AddEditPlayerActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK); // Báo cho LobbyRateVoteActivity biết để tải lại
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("AddEditPlayer", "Lỗi: ", e);
//                runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        };

        if (playerToEdit == null) {
            playerService.insertPlayer(player, callback);
        } else {
            // Đây là flow chính: Cập nhật player với điểm mới
            playerService.updatePlayer(currentUsername, currentLobbyId, player, callback);
        }
    }

    private void setSpinnerSelection(Spinner spinner, List<String> options, String value) {
        if (spinner == null || options == null || value == null) return;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    // Xóa: private void setPictureSpinnerSelection() { ... }
}