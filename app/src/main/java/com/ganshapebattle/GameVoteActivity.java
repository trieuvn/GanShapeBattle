package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
//import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.SupabaseCallback;

public class GameVoteActivity extends AppCompatActivity {

    // --- Biến cho Logic Game ---
    private LobbyService lobbyService;
    private String username;
    private String lobbyId;
    private boolean isActivityRunning = false;

    // --- Biến cho Vòng lặp Polling (Kiểm tra Status) ---
    private Handler statusCheckHandler;
    private Runnable statusCheckRunnable;
    // Kiểm tra 3 giây một lần
    private static final long CHECK_INTERVAL = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_vote);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Lấy data từ Intent ---
        username = getIntent().getStringExtra("username");
        lobbyId = getIntent().getStringExtra("lobbyid");

        if (lobbyId == null || username == null) {
//            Toast.makeText(this, "Lỗi: Thiếu thông tin Lobby hoặc User.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = new Intent(this, LobbyRateVoteActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("lobbyid", lobbyId);
        startActivity(intent);


        // --- Khởi tạo ---
        lobbyService = new LobbyService();
        statusCheckHandler = new Handler(Looper.getMainLooper());

        // Hiển thị thông báo (ví dụ, bạn có thể thêm TextView)
//        Toast.makeText(this, "Đang chờ mọi người vote...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isActivityRunning = true;
        // Bắt đầu vòng lặp kiểm tra status
        startStatusCheckLoop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityRunning = false;
        // Dừng vòng lặp
        stopStatusCheckLoop();
    }

    // ==================================================================
    // --- VÒNG LẶP KIỂM TRA STATUS (POLLING) (Theo yêu cầu) ---
    // ==================================================================

    /**
     * Bắt đầu vòng lặp kiểm tra status (3 giây 1 lần)
     */
    private void startStatusCheckLoop() {
        statusCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Chỉ chạy nếu Activity đang active
                if (!isActivityRunning || statusCheckRunnable == null) return;

                checkLobbyStatus();
            }
        };
        // Chạy lần đầu ngay lập tức
        statusCheckHandler.post(statusCheckRunnable);
    }

    /**
     * Dừng vòng lặp kiểm tra status
     */
    private void stopStatusCheckLoop() {
        if (statusCheckHandler != null && statusCheckRunnable != null) {
            statusCheckHandler.removeCallbacks(statusCheckRunnable);
            statusCheckRunnable = null;
        }
    }

    /**
     * Lên lịch kiểm tra tiếp theo
     */
    private void scheduleNextStatusCheck() {
        if (isActivityRunning && statusCheckHandler != null && statusCheckRunnable != null) {
            statusCheckHandler.postDelayed(statusCheckRunnable, CHECK_INTERVAL);
        }
    }

    /**
     * Hàm chính: Lấy Lobby từ Supabase và kiểm tra status
     */
    private void checkLobbyStatus() {
        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (!isActivityRunning) return; // Dừng nếu activity đã đóng

                if (lobby == null) {
//                    Toast.makeText(GameVoteActivity.this, "Phòng không còn tồn tại.", Toast.LENGTH_SHORT).show();
                    finish(); // Quay về
                    return;
                }

                String status = lobby.getStatus();

                // === SỬA LỖI TẠI ĐÂY ===
                // Kiểm tra "isOver" (từ logic cũ) HOẶC "isEnd" (từ LobbyRateVoteActivity)
                if ("isOver".equals(status) || "isEnd".equals(status)) {
                    // THÀNH CÔNG: Chuyển sang GameEndActivity
                    navigateToGameEnd();
                } else {
                    // Vẫn là "isVoting" hoặc trạng thái khác, "loop tiếp"
                    scheduleNextStatusCheck();
                }
                // === KẾT THÚC SỬA LỖI ===
            }

            @Override
            public void onFailure(Exception e) {
                if (!isActivityRunning) return;
                // Lỗi mạng, "loop tiếp"
//                Toast.makeText(GameVoteActivity.this, "Mất kết nối... Đang thử lại.", Toast.LENGTH_SHORT).show();
                scheduleNextStatusCheck();
            }
        });
    }

    /**
     * Hàm helper để chuyển sang GameEndActivity
     */
    private void navigateToGameEnd() {
        if (!isActivityRunning) return;

        isActivityRunning = false; // Ngăn chạy 2 lần
        stopStatusCheckLoop();

//        Toast.makeText(this, "Vote đã xong! Đang xem kết quả...", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(GameVoteActivity.this, GameEndActivity.class);
        //intent.putExtra("username", username);
        intent.putExtra("lobby_id", lobbyId);
        startActivity(intent);
        finish(); // Đóng GameVoteActivity
    }
}