package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle; // <-- Thêm

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
import java.util.List;
import java.util.Locale;

public class GameLoadActivity extends AppCompatActivity {

    private LobbyService lobbyService;
    private PlayerService playerService;
    private PictureService pictureService;
    private UserService userService;

    private String username;
    private String lobbyid;
    private Lobby currentLobby;

    // --- Biến cho Polling và Timeout ---
    private Handler checkStatusHandler;
    private Runnable checkStatusRunnable;
    private long startTimeMillis;

    private static final long CHECK_INTERVAL = 3000;
    private static final long TIMEOUT_MILLIS = 60 * 1000;

    // --- Thêm cờ (flag) để quản lý trạng thái setup ---
    private boolean isSetupComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_load);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy username từ Intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin người dùng!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        lobbyid = getIntent().getStringExtra("lobbyid");
        if (lobbyid == null || lobbyid.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin lobby!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Khởi tạo services
        lobbyService = new LobbyService();
        playerService = new PlayerService();
        pictureService = new PictureService();
        userService = new UserService();

        // Khởi tạo Handler
        checkStatusHandler = new Handler(Looper.getMainLooper());

        // --- Bắt đầu chuỗi tải dữ liệu (ĐÃ SỬA LỖI) ---
        // Bắt đầu bằng việc tải thông tin Lobby
        loadLobbyData();
    }

    /**
     * Bước 1: Tải thông tin Lobby
     */
    private void loadLobbyData() {
        lobbyService.getLobbyById(lobbyid, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby == null) {
                    handleSetupFailure(new Exception("Không tìm thấy Lobby (ID: " + lobbyid + ")"));
                    return;
                }
                currentLobby = lobby; // Lưu lobby
                // Bước 2: Tải thông tin Player
                loadPlayerData();
            }

            @Override
            public void onFailure(Exception e) {
                handleSetupFailure(e); // Lỗi, không thể tiếp tục
            }
        });
    }

    /**
     * Bước 2: Tải thông tin Player (sau khi đã tải Lobby thành công)
     */
    private void loadPlayerData() {
        playerService.getPlayerByIds(username, lobbyid, new SupabaseCallback<Player>() {
            @Override
            public void onSuccess(Player player) {
                if (player == null) {
                    handleSetupFailure(new Exception("Không tìm thấy Player (User: " + username + ")"));
                    return;
                }

                // Bước 3: Cập nhật điểm của Player (đánh dấu là "đã kết nối")
                updatePlayerStatus(player);
            }

            @Override
            public void onFailure(Exception e) {
                handleSetupFailure(e);
            }
        });
    }

    /**
     * Bước 3: Cập nhật trạng thái Player (sau khi đã tải Player thành công)
     */
    private void updatePlayerStatus(Player player) {
        player.setPoint(0); // Đánh dấu sẵn sàng
        playerService.updatePlayer(username, lobbyid, player, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Bước 4: TẤT CẢ THÀNH CÔNG
                runOnUiThread(() ->
                        Toast.makeText(GameLoadActivity.this, "Cập nhật điểm thành công", Toast.LENGTH_SHORT).show()
                );

                // Mọi thứ đã sẵn sàng, cho phép vòng lặp bắt đầu
                isSetupComplete = true;

                // Nếu onStart đã chạy trước khi setup xong, ta cần chủ động bắt đầu vòng lặp
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                    startPlayerCheckLoop();
                }
            }

            @Override
            public void onFailure(Exception e) {
                handleSetupFailure(e);
            }
        });
    }

    /**
     * Xử lý tập trung cho bất kỳ lỗi nghiêm trọng nào trong quá trình setup.
     */
    private void handleSetupFailure(Exception e) {
        runOnUiThread(() -> {
            Toast.makeText(GameLoadActivity.this, "Lỗi khi vào phòng, hãy kiểm tra internet của bạn: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Không thể tiếp tục, đóng Activity
            finish();
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Chỉ bắt đầu vòng lặp nếu quá trình setup trong onCreate đã hoàn tất
        if (isSetupComplete) {
            startPlayerCheckLoop();
        }
        // Nếu chưa, vòng lặp sẽ được 'updatePlayerStatus()' gọi sau khi hoàn tất.
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dừng vòng lặp khi Activity bị che khuất
        stopPlayerCheckLoop();
    }

    /**
     * Dừng vòng lặp kiểm tra
     */
    private void stopPlayerCheckLoop() {
        isSetupComplete = false; // Yêu cầu setup lại nếu quay lại Activity
        if (checkStatusRunnable != null) {
            checkStatusHandler.removeCallbacks(checkStatusRunnable);
        }
    }

    /**
     * Bắt đầu vòng lặp (polling) để kiểm tra trạng thái người chơi
     */
    private void startPlayerCheckLoop() {
        // Ghi lại thời điểm bắt đầu
        startTimeMillis = System.currentTimeMillis();

        runOnUiThread(() ->
                Toast.makeText(this, "Đang kiểm tra trạng thái người chơi...", Toast.LENGTH_SHORT).show()
        );

        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. Kiểm tra timeout trước
                long elapsedTime = System.currentTimeMillis() - startTimeMillis;
                if (elapsedTime >= TIMEOUT_MILLIS) {
                    handleTimeout();
                    return; // Dừng vòng lặp
                }

                // 2. Nếu chưa timeout, thực hiện kiểm tra
                checkAllPlayersReady();
            }
        };
        // Chạy lần kiểm tra đầu tiên ngay lập tức
        checkStatusHandler.post(checkStatusRunnable);
    }

    /**
     * Lấy thông tin lobby, sau đó lấy danh sách player và kiểm tra
     */
    private void checkAllPlayersReady() {
        // Chúng ta đã có currentLobby từ onCreate, không cần getLobbyById nữa
        // (Nhưng nếu bạn muốn cập nhật số người chơi real-time, bạn vẫn nên gọi lại)

        // Giữ nguyên hàm này vì nó cần lấy danh sách player mới nhất
        lobbyService.getLobbyById(lobbyid, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby == null) {
                    runOnUiThread(() -> Toast.makeText(GameLoadActivity.this, "Lỗi: Không tìm thấy phòng!", Toast.LENGTH_LONG).show());
                    scheduleNextCheck(); // Thử lại
                    return;
                }

                // Cập nhật lại currentLobby để đảm bảo dữ liệu mới nhất
                currentLobby = lobby;

                lobby.getPlayers(new SupabaseCallback<List<Player>>() {
                    @Override
                    public void onSuccess(List<Player> players) {
                        runOnUiThread(() -> {
                            if (players == null || players.isEmpty()) {
                                Toast.makeText(GameLoadActivity.this, "Chưa có người chơi...", Toast.LENGTH_SHORT).show();
                                scheduleNextCheck(); // Vẫn chờ
                                return;
                            }

                            boolean allPlayersReady = true;
                            // 3. Duyệt qua danh sách player
                            for (Player player : players) {
                                // Nếu có BẤT KỲ player nào có point = -1
                                if (player.getPoint() == -1) {
                                    allPlayersReady = false; // Đánh dấu là chưa sẵn sàng
                                    break;
                                }
                            }

                            // 4. Xử lý kết quả
                            if (allPlayersReady) {
                                // Tất cả player đều có point > -1
                                Toast.makeText(GameLoadActivity.this, "Tất cả đã sẵn sàng! Bắt đầu game...", Toast.LENGTH_LONG).show();
                                startGame();
                            } else {
                                // Vẫn còn người chưa sẵn sàng (point = -1)
                                Toast.makeText(GameLoadActivity.this, "Đang chờ người chơi khác kết nối...", Toast.LENGTH_SHORT).show();
                                scheduleNextCheck(); // Lên lịch kiểm tra tiếp
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Lỗi khi lấy danh sách player
                        runOnUiThread(() -> Toast.makeText(GameLoadActivity.this, "Lỗi kiểm tra player: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        scheduleNextCheck(); // Thử lại sau
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                // Lỗi khi lấy thông tin lobby
                runOnUiThread(() -> Toast.makeText(GameLoadActivity.this, "Lỗi lấy thông tin phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                scheduleNextCheck(); // Thử lại sau
            }
        });
    }

    /**
     * Lên lịch để chạy lại hàm kiểm tra sau CHECK_INTERVAL
     */
    private void scheduleNextCheck() {
        // Đảm bảo không lên lịch chạy nếu vòng lặp đã bị dừng
        if (checkStatusRunnable != null) {
            checkStatusHandler.postDelayed(checkStatusRunnable, CHECK_INTERVAL);
        }
    }

    /**
     * Xử lý khi hết 1 phút chờ
     */
    private void handleTimeout() {
        stopPlayerCheckLoop(); // Dừng vòng lặp

        runOnUiThread(() -> {
            Toast.makeText(GameLoadActivity.this, "Hết thời gian chờ. Hủy phòng...", Toast.LENGTH_LONG).show();

            // Vì đây là Activity của admin, chúng ta sẽ xóa lobby
            lobbyService.deleteLobby(lobbyid, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    // Xóa thành công, đóng Activity này và quay lại
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(GameLoadActivity.this, "Lỗi khi hủy phòng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish(); // Vẫn đóng Activity
                }
            });
        });
    }

    /**
     * Hàm này được gọi khi tất cả player đã sẵn sàng
     */
    private void startGame() {
        // 1. Dừng vòng lặp kiểm tra
        stopPlayerCheckLoop();

        // currentLobby đã được đảm bảo không null nhờ luồng setup trong onCreate
        if (currentLobby == null) {
            // Đây là một safeguard, về lý thuyết không bao giờ xảy ra
            handleSetupFailure(new Exception("Lỗi nghiêm trọng: Lobby bị null trước khi startGame."));
            return;
        }

        try {
            String beginDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date());
            currentLobby.setStatus("isPlaying"); // Chuyển trạng thái
            currentLobby.setBeginDate(beginDate);

            lobbyService.updateLobby(currentLobby.getId(), currentLobby, new SupabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        Toast.makeText(GameLoadActivity.this, "Trò chơi bắt đầu!", Toast.LENGTH_SHORT).show();

                        // Chuyển sang DesignActivity
                        Intent intent = new Intent(GameLoadActivity.this, DesignActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("lobbyid", currentLobby.getId());
                        startActivity(intent);

                        // 3. Đóng Activity loading này
                        //finish();
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(GameLoadActivity.this, "Lỗi bắt đầu trò chơi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    // Có lỗi, có thể thử lại hoặc hủy phòng
                    // Tạm thời, chúng ta quay lại và bắt đầu lại vòng lặp check
                    startPlayerCheckLoop();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi bắt đầu trò chơi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Xóa các lệnh gọi Intent thừa ở đây
        // Intent intent = new Intent(GameLoadActivity.this, DesignActivity.class);
        // ...
        // finish();
    }
}