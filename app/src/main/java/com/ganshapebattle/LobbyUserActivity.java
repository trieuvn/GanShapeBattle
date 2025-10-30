package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler; // <-- Thêm
import android.os.Looper;  // <-- Thêm
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LobbyUserActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText editTextLobbyId;
    private Button buttonJoin, buttonLeave;
    private LinearLayout joinLobbyLayout, inLobbyLayout;
    private TextView textViewLobbyInfo;
    private ProgressBar progressBar;

    // --- Services & Data ---
    private LobbyService lobbyService;
    private PlayerService playerService;
    private PictureService pictureService;
    private UserService userService;
    private String currentLobbyId;
    private String currentPlayerId;
    private String username;
    private User currentUser;

    // --- Thêm biến cho Polling ---
    private Handler lobbyStatusHandler;
    private Runnable lobbyStatusRunnable;
    private static final long CHECK_INTERVAL = 3000; // 3 giây
    private boolean isCheckingStatus = false; // Cờ để quản lý vòng lặp

    // --- Static method để tạo Intent với username ---
    public static Intent createIntent(android.content.Context context, String username) {
        Intent intent = new Intent(context, LobbyUserActivity.class);
        intent.putExtra("username", username);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_user);

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
        pictureService = new PictureService();
        userService = new UserService();

        bindViews();
        setupListeners();

        // Khởi tạo Handler
        lobbyStatusHandler = new Handler(Looper.getMainLooper());

        // Truy vấn thông tin user từ username
        loadUserInfo();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dừng vòng lặp khi Activity không còn hiển thị
        stopLobbyStatusCheck();
    }

    /**
     * Kiểm tra xem user hiện tại đã có trong lobby nào chưa
     */
    private void checkCurrentLobbyStatus() {
        if (currentUser == null) return;

        // Sử dụng method getPlayers() của User model để lấy danh sách players
        currentUser.getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                if (players != null && !players.isEmpty()) {
                    // User đã có trong lobby, lấy lobby đầu tiên
                    Player currentPlayer = players.get(0);
                    currentLobbyId = currentPlayer.getLobbyId();
                    currentPlayerId = currentPlayer.getUsername();

                    runOnUiThread(() -> {
                        showInLobbyView();
                        Toast.makeText(LobbyUserActivity.this, "Bạn đã ở trong phòng: " + currentLobbyId, Toast.LENGTH_SHORT).show();
                        // Bắt đầu vòng lặp kiểm tra trạng thái
                        startLobbyStatusCheck();
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Không có lỗi gì, user chưa có trong lobby nào
                runOnUiThread(() -> {
                    // Hiển thị giao diện tham gia phòng (mặc định)
                });
            }
        });
    }

    /**
     * Truy vấn thông tin user từ username
     */
    private void loadUserInfo() {
        // ... (Giữ nguyên code)
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        currentUser = user;
                        Toast.makeText(LobbyUserActivity.this, "Chào mừng " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();

                        // Sau khi load user thành công, kiểm tra xem user đã có trong lobby nào chưa
                        checkCurrentLobbyStatus();
                    } else {
                        Toast.makeText(LobbyUserActivity.this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyUserActivity.this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    /**
     * Ánh xạ các thành phần UI từ XML.
     */
    private void bindViews() {
        // ... (Giữ nguyên code)
        editTextLobbyId = findViewById(R.id.editTextLobbyId);
        buttonJoin = findViewById(R.id.buttonJoin);
        buttonLeave = findViewById(R.id.buttonLeave);
        joinLobbyLayout = findViewById(R.id.joinLobbyLayout);
        inLobbyLayout = findViewById(R.id.inLobbyLayout);
        textViewLobbyInfo = findViewById(R.id.textViewLobbyInfo);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Thiết lập các sự kiện click cho nút.
     */
    private void setupListeners() {
        buttonJoin.setOnClickListener(v -> joinLobby());
        buttonLeave.setOnClickListener(v -> leaveLobby());
    }

    /**
     * Bước 1-12: Xử lý logic khi người dùng nhấn nút "Tham gia".
     */
    private void joinLobby() {
        // ... (Giữ nguyên code)
        if (currentUser == null) {
            Toast.makeText(this, "Chưa tải được thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        String lobbyId = editTextLobbyId.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyId)) {
            Toast.makeText(this, "Vui lòng nhập mã phòng!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLobbyId != null) {
            Toast.makeText(this, "Bạn đã ở trong phòng khác!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        try {
            // Bước 3: Gọi service để lấy thông tin phòng (getById)
            lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
                @Override
                public void onSuccess(Lobby lobby) {
                    if (lobby == null) {
                        // Bước 7-8: Phòng không tồn tại
                        runOnUiThread(() -> {
                            Toast.makeText(LobbyUserActivity.this, "Phòng không tồn tại!", Toast.LENGTH_LONG).show();
                            setLoading(false);
                        });
                    } else {
                        // Bước 5: Kiểm tra slot trống
                        checkSlot(lobby);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Lỗi kiểm tra phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tham gia phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
        }
    }

    /**
     * Bước 5: Kiểm tra slot trống trong lobby
     */
    private void checkSlot(Lobby lobby) {
        // ... (Giữ nguyên code)
        // Sử dụng method getPlayers() của Lobby model để lấy danh sách players
        lobby.getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                // Kiểm tra số lượng players hiện tại với maxPlayer
                int currentPlayerCount = players.size();
                int maxPlayers = lobby.getMaxPlayer();

                if (currentPlayerCount >= maxPlayers) {
                    // Bước 7-8: Phòng đã đầy
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Phòng đã đầy! (" + currentPlayerCount + "/" + maxPlayers + ")", Toast.LENGTH_LONG).show();
                        setLoading(false);
                    });
                } else {
                    // Bước 9: Thêm người chơi mới
                    insertPlayer(lobby.getId());
                }
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyUserActivity.this, "Lỗi kiểm tra slot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                });
            }
        });
    }

    /**
     * Bước 9: Thêm người chơi mới vào lobby
     */
    private void insertPlayer(String lobbyId) {
        createPictureForPlayer(lobbyId);
    }

    /**
     * Tạo default picture cho player
     */
    private Picture createDefaultPicture(String lobbyId) {
        // Sử dụng format ISO 8601 chuẩn cho PostgreSQL
        String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());
        // Tạo ID đơn giản hơn, tránh ký tự đặc biệt
        String pictureId = "pic_" + currentUser.getUsername().replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis();

        Picture defaultPicture = new Picture();
        //defaultPicture.setId(pictureId);
        defaultPicture.setName("Default Picture - " + currentUser.getUsername());
        defaultPicture.setDescription("Default picture created when joining lobby " + lobbyId);
        defaultPicture.setCreatedDate(currentTime);
        defaultPicture.setType("default"); // Thay đổi type thành "default" thay vì "game_picture"
        defaultPicture.setImage(""); // Sử dụng empty string thay vì null
        defaultPicture.setTags("default,lobby,game,placeholder");
        defaultPicture.setGalleryId("d819cf07-9afc-4089-8d50-d07ce29d47c2"); // Sử dụng empty string thay vì null
        defaultPicture.setUsername(currentUser.getUsername());

        return defaultPicture;
    }

    /**
     * Tạo picture cho player trước khi tạo player
     */
    private void createPictureForPlayer(String lobbyId) {
        Picture newPicture = createDefaultPicture(lobbyId);

        pictureService.insertPicture(newPicture, new SupabaseCallback<Picture>() {
            @Override
            public void onSuccess(Picture insertedPicture) {
                // Bước 9b: Sau khi tạo picture thành công, tạo player với pictureId từ Supabase
                createPlayerWithPicture(lobbyId, insertedPicture.getId());
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyUserActivity.this, "Lỗi tạo picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    editTextLobbyId.setText("Lỗi tạo picture: " + e.getMessage());
                    setLoading(false);
                });
            }
        });
    }

    /**
     * Tạo player với pictureId đã tạo
     */
    private void createPlayerWithPicture(String lobbyId, String pictureId) {
        Player newPlayer = new Player();
        newPlayer.setUsername(currentUser.getUsername());
        newPlayer.setLobbyId(lobbyId);
        newPlayer.setPoint(-1); // Point = -1 khi mới tham gia
        newPlayer.setRank(0);
        newPlayer.setPictureId(pictureId); // Sử dụng pictureId vừa tạo

        playerService.insertPlayer(newPlayer, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Bước 11-12: Thành công, chuyển hướng giao diện
                runOnUiThread(() -> {
                    currentLobbyId = lobbyId;
                    currentPlayerId = currentUser.getUsername(); // Sử dụng username làm player ID
                    setLoading(false);
                    showInLobbyView();
                    Toast.makeText(LobbyUserActivity.this, "Tham gia thành công! Đang chờ admin...", Toast.LENGTH_SHORT).show();

                    // BẮT ĐẦU VÒNG LẶP KIỂM TRA
                    startLobbyStatusCheck();
                });
            }

            @Override
            public void onFailure(Exception e) {
                // ... (Giữ nguyên code)
                runOnUiThread(() -> {
                    String errorMessage = "Không thể tham gia phòng: " + e.getMessage();
                    if (e.getMessage().contains("23503")) {
                        errorMessage += "\nLỗi: Foreign key constraint - Kiểm tra dữ liệu liên quan";
                    }
                    Toast.makeText(LobbyUserActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    editTextLobbyId.setText(errorMessage);
                    setLoading(false);
                });
            }
        });
    }

    /**
     * Bước 13-18: Xử lý logic khi người dùng nhấn nút "Rời phòng".
     */
    private void leaveLobby() {
        // DỪNG VÒNG LẶP NGAY LẬP TỨC
        stopLobbyStatusCheck();

        if (currentPlayerId == null || currentLobbyId == null) {
            Toast.makeText(this, "Bạn chưa ở trong phòng nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        try {
            // Bước 15: Gọi service để xóa người chơi (delete(Player))
            playerService.deletePlayer(currentUser.getUsername(), currentLobbyId, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Bước 17-18: Thành công, chuyển hướng giao diện
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Đã rời phòng.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        showJoinView();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Lỗi khi rời phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi rời phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
        }
    }

    /**
     * Hiển thị giao diện khi đã ở trong phòng.
     */
    private void showInLobbyView() {
        // ... (Giữ nguyên code)
        joinLobbyLayout.setVisibility(View.GONE);
        inLobbyLayout.setVisibility(View.VISIBLE);

        // Hiển thị thông tin lobby chi tiết
        displayLobbyInfo();
    }

    /**
     * Hiển thị thông tin chi tiết của lobby
     */
    private void displayLobbyInfo() {
        // ... (Giữ nguyên code)
        if (currentLobbyId == null) {
            textViewLobbyInfo.setText("Mã phòng: Không xác định");
            return;
        }

        // Lấy thông tin lobby từ database
        lobbyService.getLobbyById(currentLobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby != null) {
                    runOnUiThread(() -> {
                        String lobbyInfo = "Mã phòng: " + lobby.getId() + "\n" +
                                "Chế độ: " + (lobby.getMode().equals("vote") ? "Vote" : "Rate") + "\n" +
                                "Trạng thái: " + getStatusText(lobby.getStatus()) + "\n" + // Cập nhật trạng thái ở đây
                                "Số người tối đa: " + lobby.getMaxPlayer() + "\n" +
                                "Thời gian thiết kế: " + lobby.getDesignTime() + " phút\n" +
                                "Thời gian vote: " + lobby.getVoteTime() + " phút";
                        textViewLobbyInfo.setText(lobbyInfo);

                        // Lấy thông tin admin của lobby
                        displayAdminInfo(lobby);
                    });
                } else {
                    runOnUiThread(() -> {
                        textViewLobbyInfo.setText("Mã phòng: " + currentLobbyId + "\n(Lỗi tải thông tin)");
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    textViewLobbyInfo.setText("Mã phòng: " + currentLobbyId + "\n(Lỗi: " + e.getMessage() + ")");
                });
            }
        });
    }

    /**
     * Hiển thị thông tin admin của lobby
     */
    private void displayAdminInfo(Lobby lobby) {
        // ... (Giữ nguyên code)
        // Sử dụng method getAdmin() của Lobby model để lấy thông tin admin
        lobby.getAdmin(new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User admin) {
                if (admin != null) {
                    runOnUiThread(() -> {
                        String currentInfo = textViewLobbyInfo.getText().toString();
                        String adminInfo = "\nAdmin: " + admin.getUsername() +
                                (admin.getEmail() != null ? " (" + admin.getEmail() + ")" : "");
                        textViewLobbyInfo.setText(currentInfo + adminInfo);
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Không hiển thị thông tin admin nếu có lỗi
            }
        });
    }

    /**
     * Chuyển đổi status code thành text hiển thị
     */
    private String getStatusText(String status) {
        if (status == null) return "Không xác định";

        switch (status) {
            case "waiting":
                return "Đang chờ";
            case "loading":
                return "Đang load";
            case "isPlaying":
                return "Đang thiết kế";
            case "isVoting":
                return "Đang vote";
            case "isOver":
                return "Kết thúc";
            case "isEnd":
                return "Hoàn thành";
            default:
                return status;
        }
    }

    /**
     * Reset và hiển thị lại giao diện tham gia phòng ban đầu.
     */
    private void showJoinView() {
        // ... (GiGitữ nguyên code)
        inLobbyLayout.setVisibility(View.GONE);
        joinLobbyLayout.setVisibility(View.VISIBLE);
        editTextLobbyId.setText("");
        this.currentLobbyId = null;
        this.currentPlayerId = null;
    }

    /**
     * Hiển thị hoặc ẩn ProgressBar.
     * @param isLoading Trạng thái loading.
     */
    private void setLoading(boolean isLoading) {
        // ... (Giữ nguyên code)
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonJoin.setEnabled(false);
            buttonLeave.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonJoin.setEnabled(true);
            buttonLeave.setEnabled(true);
        }
    }

    /**
     * Kiểm tra xem phòng có đầy không (helper method cho Supabase).
     * @param lobbyData Dữ liệu phòng từ Supabase
     * @return true nếu phòng đầy, false nếu còn chỗ
     */
    private boolean isLobbyFull(HashMap<String, Object> lobbyData) {
        // ... (Giữ nguyên code)
        // Giả sử có field max_players và current_players trong lobby data
        // int maxPlayers = (Integer) lobbyData.get("max_players");
        // int currentPlayers = (Integer) lobbyData.get("current_players");
        // return currentPlayers >= maxPlayers;

        // Tạm thời return false để giả lập
        return false;
    }

    // ==================================================================
    // --- PHẦN CODE MỚI: VÒNG LẶP KIỂM TRA TRẠNG THÁI LOBBY ---
    // ==================================================================

    /**
     * Bắt đầu vòng lặp kiểm tra trạng thái lobby
     */
    private void startLobbyStatusCheck() {
        if (isCheckingStatus) return; // Đã chạy rồi thì thôi

        isCheckingStatus = true;

        lobbyStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCheckingStatus) return; // Dừng nếu cờ đã tắt

                // Lấy thông tin lobby và kiểm tra
                fetchAndCheckLobbyStatus();
            }
        };
        // Chạy lần đầu ngay lập tức
        lobbyStatusHandler.post(lobbyStatusRunnable);
    }

    /**
     * Dừng vòng lặp kiểm tra
     */
    private void stopLobbyStatusCheck() {
        isCheckingStatus = false;
        if (lobbyStatusHandler != null) {
            lobbyStatusHandler.removeCallbacks(lobbyStatusRunnable);
        }
    }

    /**
     * Lấy thông tin lobby từ Supabase và kiểm tra status
     */
    private void fetchAndCheckLobbyStatus() {
        if (currentLobbyId == null) {
            stopLobbyStatusCheck();
            return;
        }

        lobbyService.getLobbyById(currentLobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby == null) {
                    // Lobby không còn tồn tại (có thể bị admin xóa)
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Phòng đã bị hủy!", Toast.LENGTH_LONG).show();
                        stopLobbyStatusCheck();
                        showJoinView(); // Quay về màn hình tham gia
                    });
                    return;
                }

                String status = lobby.getStatus();

                // Cập nhật lại UI thông tin (để người dùng thấy status thay đổi)
                runOnUiThread(() -> displayLobbyInfo());
                if (status == null){
                    scheduleNextStatusCheck();
                }
                if ("loading".equals(status)) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Admin đã bắt đầu! Đang tải...", Toast.LENGTH_SHORT).show();
                        stopLobbyStatusCheck();

                        Intent intent = new Intent(LobbyUserActivity.this, GameLoadActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("lobbyid", currentLobbyId);
                        startActivity(intent);
                        finish(); // Đóng Activity này
                    });

                    //Giả định 2: Nếu game đã thực sự bắt đầu (ví dụ status là "isPlaying")
                } else if ("isPlaying".equals(status)) {
                    runOnUiThread(() -> {
                        Toast.makeText(LobbyUserActivity.this, "Trò chơi đã bắt đầu! Vào thẳng...", Toast.LENGTH_SHORT).show();
                        stopLobbyStatusCheck();
                        Intent intent = new Intent(LobbyUserActivity.this, DesignActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("lobbyid", currentLobbyId);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    // Các trạng thái khác (isVoting, isOver...) -> tiếp tục chờ
                    scheduleNextStatusCheck();
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Có lỗi mạng, thử lại sau
                scheduleNextStatusCheck();
            }
        });
    }

    /**
     * Lên lịch kiểm tra tiếp theo
     */
    private void scheduleNextStatusCheck() {
        if (isCheckingStatus) {
            lobbyStatusHandler.postDelayed(lobbyStatusRunnable, CHECK_INTERVAL);
        }
    }
}