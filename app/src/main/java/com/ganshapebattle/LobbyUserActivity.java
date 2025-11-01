// main/java/com/ganshapebattle/LobbyUserActivity.java
package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
//import android.widget.Toast;

// --- THÊM CÁC IMPORT SAU ---
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.textfield.TextInputLayout;
// --- KẾT THÚC THÊM IMPORT ---

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LobbyUserActivity extends AppCompatActivity {

    private static final String TAG = "LobbyUserActivity";

    // --- UI Elements ---
    private EditText editTextLobbyId;
    private Button buttonJoin, buttonLeave;
    private LinearLayout joinLobbyLayout, inLobbyLayout;
    private TextView textViewLobbyInfo;
    private ProgressBar progressBar;
    private TextInputLayout layoutLobbyId; // <-- THÊM MỚI

    // --- THÊM MỚI: ActivityResultLauncher cho QR Scan ---
    private ActivityResultLauncher<Intent> qrScanLauncher;
    // --- KẾT THÚC THÊM MỚI ---

    // ... (Các biến Services & Data giữ nguyên) ...
    private LobbyService lobbyService;
    private PlayerService playerService;
    private PictureService pictureService;
    private UserService userService;
    private String currentLobbyId;
    private String currentPlayerId;
    private String username;
    private User currentUser;
    private Handler lobbyStatusHandler;
    private Runnable lobbyStatusRunnable;
    private static final long CHECK_INTERVAL = 2000;
    private boolean isCheckingStatus = false;
    private volatile boolean userManuallyJoined = false;


    public static Intent createIntent(android.content.Context context, String username) {
        Intent intent = new Intent(context, LobbyUserActivity.class);
        intent.putExtra("username", username);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_user);

        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
//            Toast.makeText(this, "Lỗi: Không có thông tin người dùng!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- THÊM MỚI: Đăng ký QR Launcher ---
        // Phải đăng ký trước khi setupListeners
        qrScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // QrScanActivity trả về text trong EXTRA_QR_TEXT
                        String scannedId = result.getData().getStringExtra(QrScanActivity.EXTRA_QR_TEXT);

                        if (scannedId != null && !scannedId.isEmpty()) {
                            Log.d(TAG, "QR Scan Result: " + scannedId);
                            // Tự động điền vào EditText
                            editTextLobbyId.setText(scannedId);
                            // Tự động nhấn "Tham gia"
                            // Toast.makeText(this, "Đã quét mã: " + scannedId + ". Đang tham gia...", Toast.LENGTH_SHORT).show();
                            joinLobby(); // Tự động gọi join
                        } else {
                            // Toast.makeText(this, "Không tìm thấy mã lobby từ QR", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Toast.makeText(this, "Hủy quét QR", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        // --- KẾT THÚC THÊM MỚI ---

        lobbyService = new LobbyService();
        playerService = new PlayerService();
        pictureService = new PictureService();
        userService = new UserService();

        bindViews();
        setupListeners();

        lobbyStatusHandler = new Handler(Looper.getMainLooper());
        loadUserInfo();
    }

    // ... (Hàm onStop giữ nguyên) ...
    @Override
    protected void onStop() {
        super.onStop();
        stopLobbyStatusCheck();
    }


    // ... (Hàm checkCurrentLobbyStatus và findAndJoinFirstActiveLobby giữ nguyên) ...
    private void checkCurrentLobbyStatus() {
        if (currentUser == null) return;
        userManuallyJoined = false;

        currentUser.getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                if (userManuallyJoined) return;

                if (players == null || players.isEmpty()) {
                    Log.d(TAG, "checkCurrentLobbyStatus: Người dùng không ở trong phòng nào.");
                    return;
                }

                Log.d(TAG, "checkCurrentLobbyStatus: Người dùng đang ở trong " + players.size() + " phòng. Bắt đầu kiểm tra...");
                findAndJoinFirstActiveLobby(new java.util.ArrayList<>(players));
            }
            @Override
            public void onFailure(Exception e) {
                if (userManuallyJoined) return;
                Log.e(TAG, "Lỗi khi lấy danh sách player: ", e);
            }
        });
    }

    private void findAndJoinFirstActiveLobby(List<Player> playersToCheck) {
        if (userManuallyJoined) {
            Log.d(TAG, "findAndJoinFirstActiveLobby: Dừng kiểm tra (userManuallyJoined=true).");
            return;
        }

        if (playersToCheck.isEmpty()) {
            Log.d(TAG, "findAndJoinFirstActiveLobby: Đã kiểm tra hết, không tìm thấy phòng nào active.");
            return;
        }

        Player potentialPlayer = playersToCheck.remove(0);
        String potentialLobbyId = potentialPlayer.getLobbyId();
        Log.d(TAG, "findAndJoinFirstActiveLobby: Đang kiểm tra Lobby ID: " + potentialLobbyId);

        lobbyService.getLobbyById(potentialLobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (userManuallyJoined) {
                    Log.d(TAG, "findAndJoinFirstActiveLobby (onSuccess): Dừng kiểm tra (userManuallyJoined=true).");
                    return;
                }

                if (lobby != null) {
                    String status = lobby.getStatus();
                    boolean isActive = "waiting".equals(status) ||
                            "loading".equals(status) ||
                            "isPlaying".equals(status) ||
                            "isVoting".equals(status);

                    if (isActive) {
                        Log.d(TAG, "findAndJoinFirstActiveLobby: Tìm thấy phòng active: " + potentialLobbyId + " (Status: " + status + "). Sẽ lưu lại nhưng không tự động join.");
                        currentLobbyId = potentialLobbyId;
                        currentPlayerId = potentialPlayer.getUsername();
                    } else {
                        Log.w(TAG, "findAndJoinFirstActiveLobby: Bỏ qua phòng đã kết thúc: " + potentialLobbyId);
                        findAndJoinFirstActiveLobby(playersToCheck); // Đệ quy
                    }
                } else {
                    Log.e(TAG, "findAndJoinFirstActiveLobby: Player record trỏ đến lobby không tồn tại: " + potentialLobbyId);
                    findAndJoinFirstActiveLobby(playersToCheck); // Đệ quy
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (userManuallyJoined) {
                    Log.d(TAG, "findAndJoinFirstActiveLobby (onFailure): Dừng kiểm tra (userManuallyJoined=true).");
                    return;
                }
                Log.e(TAG, "findAndJoinFirstActiveLobby: Lỗi khi kiểm tra lobby " + potentialLobbyId, e);
                findAndJoinFirstActiveLobby(playersToCheck); // Đệ quy
            }
        });
    }


    // ... (Hàm joinLobby và proceedToJoinLobby giữ nguyên) ...
    private void joinLobby() {
        if (currentUser == null) {
//            Toast.makeText(this, "Chưa tải được thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        userManuallyJoined = true;

        String newLobbyId = editTextLobbyId.getText().toString().trim();
        if (TextUtils.isEmpty(newLobbyId)) {
//            Toast.makeText(this, "Vui lòng nhập mã phòng!", Toast.LENGTH_SHORT).show();
            userManuallyJoined = false; // Reset cờ nếu nhập liệu sai
            return;
        }

        setLoading(true);
        String oldLobbyId = this.currentLobbyId;

        if (oldLobbyId != null && !oldLobbyId.equals(newLobbyId)) {
            Log.d(TAG, "joinLobby: Đang ở phòng " + oldLobbyId + ". Rời phòng cũ trước khi vào " + newLobbyId);
            stopLobbyStatusCheck();

            playerService.deletePlayer(currentUser.getUsername(), oldLobbyId, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "joinLobby: Rời phòng cũ thành công. Đang tham gia phòng mới: " + newLobbyId);
                    LobbyUserActivity.this.currentLobbyId = null;
                    LobbyUserActivity.this.currentPlayerId = null;
                    proceedToJoinLobby(newLobbyId);
                }
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Lỗi khi rời phòng cũ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                        userManuallyJoined = false; // Reset cờ nếu lỗi
                    });
                }
            });
        } else if (oldLobbyId != null && oldLobbyId.equals(newLobbyId)) {
            Log.d(TAG, "joinLobby: Người dùng nhập lại mã phòng đang ở.");
            runOnUiThread(() -> {
//                Toast.makeText(this, "Bạn đã ở trong phòng này rồi!", Toast.LENGTH_SHORT).show();
                setLoading(false);
                showInLobbyView();
                startLobbyStatusCheck();
            });
        } else {
            Log.d(TAG, "joinLobby: Không ở phòng nào. Tham gia phòng mới: " + newLobbyId);
            proceedToJoinLobby(newLobbyId); // Gọi hàm logic join
        }
    }

    private void proceedToJoinLobby(String lobbyId) {
        try {
            lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
                @Override
                public void onSuccess(Lobby lobby) {
                    if (lobby == null) {
                        runOnUiThread(() -> {
//                            Toast.makeText(LobbyUserActivity.this, "Phòng không tồn tại!", Toast.LENGTH_LONG).show();
                            setLoading(false);
                            userManuallyJoined = false; // Reset cờ nếu lỗi
                        });
                    } else {
                        checkSlot(lobby);
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Lỗi kiểm tra phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                        userManuallyJoined = false; // Reset cờ nếu lỗi
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(this, "Lỗi tham gia phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
            userManuallyJoined = false; // Reset cờ nếu lỗi
        }
    }


    // ... (Hàm loadUserInfo giữ nguyên) ...
    private void loadUserInfo() {
        userService.getUserByUsername(username, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    if (user != null) {
                        currentUser = user;
//                        Toast.makeText(LobbyUserActivity.this, "Chào mừng " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();
                        checkCurrentLobbyStatus();
                    } else {
//                        Toast.makeText(LobbyUserActivity.this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
//                    Toast.makeText(LobbyUserActivity.this, "Lỗi tải thông tin người dùng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    /**
     * Ánh xạ các thành phần UI từ XML. (ĐÃ SỬA)
     */
    private void bindViews() {
        editTextLobbyId = findViewById(R.id.editTextLobbyId);
        buttonJoin = findViewById(R.id.buttonJoin);
        buttonLeave = findViewById(R.id.buttonLeave);
        joinLobbyLayout = findViewById(R.id.joinLobbyLayout);
        inLobbyLayout = findViewById(R.id.inLobbyLayout);
        textViewLobbyInfo = findViewById(R.id.textViewLobbyInfo);
        progressBar = findViewById(R.id.progressBar);
        layoutLobbyId = findViewById(R.id.layoutLobbyId); // <-- THÊM MỚI
    }

    /**
     * Thiết lập các sự kiện click cho nút. (ĐÃ SỬA)
     */
    private void setupListeners() {
        buttonJoin.setOnClickListener(v -> joinLobby());
        buttonLeave.setOnClickListener(v -> leaveLobby());

        // --- THÊM MỚI: Sự kiện click cho icon quét QR ---
        if (layoutLobbyId != null) {
            layoutLobbyId.setEndIconOnClickListener(v -> {
                // Mở QrScanActivity
                Intent intent = new Intent(LobbyUserActivity.this, QrScanActivity.class);
                qrScanLauncher.launch(intent);
            });
        }
        // --- KẾT THÚC THÊM MỚI ---
    }

    // ... (Các hàm checkSlot, insertPlayer, createDefaultPicture, createPictureForPlayer,
    //      leaveLobby, showInLobbyView, displayLobbyInfo, displayAdminInfo,
    //      getStatusText, showJoinView, setLoading, isLobbyFull,
    //      startLobbyStatusCheck, stopLobbyStatusCheck,
    //      fetchAndCheckLobbyStatus, scheduleNextStatusCheck
    //      GIỮ NGUYÊN NHƯ FILE TRƯỚC) ...

    private void checkSlot(Lobby lobby) {
        lobby.getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                int currentPlayerCount = players.size();
                int maxPlayers = lobby.getMaxPlayer();
                if (currentPlayerCount >= maxPlayers) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Phòng đã đầy! (" + currentPlayerCount + "/" + maxPlayers + ")", Toast.LENGTH_LONG).show();
                        setLoading(false);
                        userManuallyJoined = false;
                    });
                } else {
                    insertPlayer(lobby.getId());
                }
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
//                    Toast.makeText(LobbyUserActivity.this, "Lỗi kiểm tra slot: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    setLoading(false);
                    userManuallyJoined = false;
                });
            }
        });
    }

    private void insertPlayer(String lobbyId) {
        createPictureForPlayer(lobbyId);
    }

    private Picture createDefaultPicture(String lobbyId) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(new Date());
        Picture defaultPicture = new Picture();
        defaultPicture.setName("Default Picture - " + currentUser.getUsername());
        defaultPicture.setDescription("Default picture created when joining lobby " + lobbyId);
        defaultPicture.setCreatedDate(currentTime);
        defaultPicture.setType("default");
        defaultPicture.setImage("");
        defaultPicture.setTags("default,lobby,game,placeholder");
        defaultPicture.setGalleryId("d819cf07-9afc-4089-8d50-d07ce29d47c2");
        defaultPicture.setUsername(currentUser.getUsername());
        return defaultPicture;
    }

    private void createPictureForPlayer(String lobbyId) {
        Picture newPicture = createDefaultPicture(lobbyId);
        pictureService.insertPicture(newPicture, new SupabaseCallback<Picture>() {
            @Override
            public void onSuccess(Picture insertedPicture) {
                createPlayerWithPicture(lobbyId, insertedPicture.getId());
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
//                    Toast.makeText(LobbyUserActivity.this, "Lỗi tạo picture: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    editTextLobbyId.setText("Lỗi tạo picture: " + e.getMessage());
                    setLoading(false);
                    userManuallyJoined = false;
                });
            }
        });
    }

    private void createPlayerWithPicture(String lobbyId, String pictureId) {
        Player newPlayer = new Player();
        newPlayer.setUsername(currentUser.getUsername());
        newPlayer.setLobbyId(lobbyId);
        newPlayer.setPoint(-1);
        newPlayer.setRank(0);
        newPlayer.setPictureId(pictureId);

        playerService.insertPlayer(newPlayer, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    currentLobbyId = lobbyId;
                    currentPlayerId = currentUser.getUsername();
                    setLoading(false);
                    showInLobbyView();
//                    Toast.makeText(LobbyUserActivity.this, "Tham gia thành công! Đang chờ admin...", Toast.LENGTH_SHORT).show();
                    startLobbyStatusCheck();
                });
            }
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    String errorMessage = "Không thể tham gia phòng: " + e.getMessage();
//                    Toast.makeText(LobbyUserActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    editTextLobbyId.setText(errorMessage);
                    setLoading(false);
                    userManuallyJoined = false;
                });
            }
        });
    }

    private void leaveLobby() {
        stopLobbyStatusCheck();
        userManuallyJoined = false;

        if (currentPlayerId == null || currentLobbyId == null) {
//            Toast.makeText(this, "Bạn chưa ở trong phòng nào!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        try {
            playerService.deletePlayer(currentUser.getUsername(), currentLobbyId, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Đã rời phòng.", Toast.LENGTH_SHORT).show();
                        setLoading(false);
                        showJoinView();
                    });
                }
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Lỗi khi rời phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setLoading(false);
                        startLobbyStatusCheck();
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(this, "Lỗi khi rời phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
            startLobbyStatusCheck();
        }
    }

    private void showInLobbyView() {
        joinLobbyLayout.setVisibility(View.GONE);
        inLobbyLayout.setVisibility(View.VISIBLE);
        displayLobbyInfo();
    }

    private void displayLobbyInfo() {
        if (currentLobbyId == null) {
            textViewLobbyInfo.setText("Mã phòng: Không xác định");
            return;
        }

        lobbyService.getLobbyById(currentLobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby != null) {
                    runOnUiThread(() -> {
                        String lobbyInfo = "Mã phòng: " + lobby.getId() + "\n" +
                                "Chế độ: " + (lobby.getMode().equals("vote") ? "Vote" : "Rate") + "\n" +
                                "Trạng thái: " + getStatusText(lobby.getStatus()) + "\n" +
                                "Số người tối đa: " + lobby.getMaxPlayer() + "\n" +
                                "Thời gian thiết kế: " + lobby.getDesignTime() + " phút\n" +
                                "Thời gian vote: " + lobby.getVoteTime() + " phút";
                        textViewLobbyInfo.setText(lobbyInfo);
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

    private void displayAdminInfo(Lobby lobby) {
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
            public void onFailure(Exception e) {}
        });
    }

    private String getStatusText(String status) {
        if (status == null) return "Không xác định";
        switch (status) {
            case "waiting": return "Đang chờ";
            case "loading": return "Đang load";
            case "isPlaying": return "Đang thiết kế";
            case "isVoting": return "Đang vote";
            case "isOver": return "Kết thúc";
            case "isEnd": return "Hoàn thành";
            default: return status;
        }
    }

    private void showJoinView() {
        inLobbyLayout.setVisibility(View.GONE);
        joinLobbyLayout.setVisibility(View.VISIBLE);
        editTextLobbyId.setText("");
        this.currentLobbyId = null;
        this.currentPlayerId = null;
    }

    private void setLoading(boolean isLoading) {
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

    private boolean isLobbyFull(HashMap<String, Object> lobbyData) {
        return false;
    }

    private void startLobbyStatusCheck() {
        if (isCheckingStatus) return;
        isCheckingStatus = true;

        lobbyStatusRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isCheckingStatus) return;
                fetchAndCheckLobbyStatus();
            }
        };
        lobbyStatusHandler.post(lobbyStatusRunnable);
    }

    private void stopLobbyStatusCheck() {
        isCheckingStatus = false;
        if (lobbyStatusHandler != null) {
            lobbyStatusHandler.removeCallbacks(lobbyStatusRunnable);
        }
    }

    private void fetchAndCheckLobbyStatus() {
        if (currentLobbyId == null) {
            stopLobbyStatusCheck();
            return;
        }

        lobbyService.getLobbyById(currentLobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby == null) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Phòng đã bị hủy!", Toast.LENGTH_LONG).show();
                        stopLobbyStatusCheck();
                        showJoinView();
                    });
                    return;
                }

                String status = lobby.getStatus();
                runOnUiThread(() -> displayLobbyInfo());

                if (status == null){
                    scheduleNextStatusCheck();
                    return;
                }

                if ("loading".equals(status)) {
                    runOnUiThread(() -> {
//                        Toast.makeText(LobbyUserActivity.this, "Admin đã bắt đầu! Đang tải...", Toast.LENGTH_SHORT).show();
                        stopLobbyStatusCheck();
                        Intent intent = new Intent(LobbyUserActivity.this, GameLoadActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("lobbyid", currentLobbyId);
                        startActivity(intent);
                        finish();
                    });
                } else if ("isPlaying".equals(status)) {
                    String beginVoteDate = lobby.getBeginVoteDate();
                    if (beginVoteDate == null) {
                        runOnUiThread(() -> {
//                            Toast.makeText(LobbyUserActivity.this, "Đang đồng bộ thời gian với phòng...", Toast.LENGTH_SHORT).show();
                        });
                        scheduleNextStatusCheck();
                    } else {
                        runOnUiThread(() -> {
//                            Toast.makeText(LobbyUserActivity.this, "Trò chơi đã bắt đầu! Vào thẳng...", Toast.LENGTH_SHORT).show();
                            stopLobbyStatusCheck();
                            Intent intent = new Intent(LobbyUserActivity.this, DesignActivity.class);
                            intent.putExtra("username", username);
                            intent.putExtra("lobbyid", currentLobbyId);
                            intent.putExtra("votetime", beginVoteDate);
                            startActivity(intent);
                            finish();
                        });
                    }
                } else {
                    scheduleNextStatusCheck();
                }
            }
            @Override
            public void onFailure(Exception e) {
                scheduleNextStatusCheck();
            }
        });
    }

    private void scheduleNextStatusCheck() {
        if (isCheckingStatus) {
            lobbyStatusHandler.postDelayed(lobbyStatusRunnable, CHECK_INTERVAL);
        }
    }
}