package com.ganshapebattle;

import android.graphics.Bitmap; // <-- Thêm
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast; // <-- Thêm
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// import com.bumptech.glide.Glide; // <-- Xóa Glide
import com.ganshapebattle.adapters.LeaderboardAdapter;
import com.ganshapebattle.models.Lobby; // <-- Thêm
import com.ganshapebattle.models.Picture; // <-- Thêm
import com.ganshapebattle.models.Player; // <-- Thêm
import com.ganshapebattle.models.PlayerScore;
import com.ganshapebattle.services.LobbyService; // <-- Thêm
import com.ganshapebattle.services.PlayerService; // <-- Thêm
import com.ganshapebattle.services.SupabaseCallback; // <-- Thêm
import com.ganshapebattle.utils.ImageUtils; // <-- Thêm

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // <-- Thêm
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger; // <-- Thêm
import java.util.stream.Collectors; // <-- Thêm

public class Leaderboard extends AppCompatActivity {

    private String lobbyId;
    private TextView tvTitle;

    // Top 5 Views
    private ImageView[] ivAvatars = new ImageView[5];
    private TextView[] tvPlayerNames = new TextView[5];
    private TextView[] tvScores = new TextView[5];
    private View[] playerContainers = new View[5];

    // Other Players
    private ListView lvOtherPlayers;
    private LeaderboardAdapter otherPlayersAdapter;
    private ArrayList<PlayerScore> otherPlayersList;

    // Services
    private PlayerService playerService;
    private LobbyService lobbyService;

    private static final String TAG = "LeaderboardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy lobbyId từ Intent
        lobbyId = getIntent().getStringExtra("lobby_id");
        if (lobbyId == null || lobbyId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin lobby!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Khởi tạo Services
        playerService = new PlayerService();
        lobbyService = new LobbyService();

        // Ánh xạ UI
        bindViews();

        // Khởi tạo Adapter
        otherPlayersList = new ArrayList<>();
        otherPlayersAdapter = new LeaderboardAdapter(this, otherPlayersList);
        lvOtherPlayers.setAdapter(otherPlayersAdapter);

        // Bắt đầu tải dữ liệu
        loadLeaderboardData();
    }

    /**
     * Tải dữ liệu bất đồng bộ: Players -> Pictures -> PlayerScores
     */
    private void loadLeaderboardData() {
        Toast.makeText(this, "Đang tải Bảng xếp hạng...", Toast.LENGTH_SHORT).show();

        // 1. Tải tất cả Players
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                // 2. Lọc ra player chỉ thuộc lobby này
                List<Player> lobbyPlayers = allPlayers.stream()
                        .filter(p -> lobbyId.equals(p.getLobbyId()))
                        .collect(Collectors.toList());

                if (lobbyPlayers.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(Leaderboard.this, "Chưa có ai trong phòng này", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 3. Chuẩn bị tải ảnh (N+1 query)
                List<PlayerScore> playerScores = new ArrayList<>();
                AtomicInteger counter = new AtomicInteger(0); // Bộ đếm an toàn

                for (Player player : lobbyPlayers) {
                    // 4. Với mỗi Player, tải Picture của họ
                    player.getPicture(new SupabaseCallback<Picture>() {
                        @Override
                        public void onSuccess(Picture picture) {
                            // 5. Tạo PlayerScore
                            PlayerScore ps = new PlayerScore();
                            ps.setUsername(player.getUsername());
                            ps.setScore(player.getPoint());

                            if (picture != null && picture.getImage() != null) {
                                ps.setPictureBase64(picture.getImage());
                            } else {
                                ps.setPictureBase64(null); // Không có ảnh
                            }
                            playerScores.add(ps);

                            // 6. Kiểm tra xem đã tải xong tất cả chưa
                            if (counter.incrementAndGet() == lobbyPlayers.size()) {
                                // Đã tải xong, sắp xếp và hiển thị
                                runOnUiThread(() -> {
                                    // Sắp xếp theo điểm, cao nhất trước
                                    Collections.sort(playerScores, (o1, o2) -> Integer.compare(o2.getScore(), o1.getScore()));
                                    populateLeaderboard(playerScores);
                                });
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Lỗi tải Picture cho " + player.getUsername(), e);
                            // Vẫn phải đếm, nếu không sẽ bị kẹt
                            if (counter.incrementAndGet() == lobbyPlayers.size()) {
                                runOnUiThread(() -> {
                                    Collections.sort(playerScores, (o1, o2) -> Integer.compare(o2.getScore(), o1.getScore()));
                                    populateLeaderboard(playerScores);
                                });
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải Players: ", e);
                runOnUiThread(() -> Toast.makeText(Leaderboard.this, "Lỗi tải danh sách player: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }


    /**
     * Ánh xạ các views từ XML
     */
    private void bindViews() {
        tvTitle = findViewById(R.id.tvTitle);

        // Top 3 (Podium)
        ivAvatars[0] = findViewById(R.id.ivPlayerAvatar1);
        tvPlayerNames[0] = findViewById(R.id.tvPlayerName1);
        tvScores[0] = findViewById(R.id.tvScore1);
        playerContainers[0] = findViewById(R.id.player1_card);

        ivAvatars[1] = findViewById(R.id.ivPlayerAvatar2);
        tvPlayerNames[1] = findViewById(R.id.tvPlayerName2);
        tvScores[1] = findViewById(R.id.tvScore2);
        playerContainers[1] = findViewById(R.id.player2_card);

        ivAvatars[2] = findViewById(R.id.ivPlayerAvatar3);
        tvPlayerNames[2] = findViewById(R.id.tvPlayerName3);
        tvScores[2] = findViewById(R.id.tvScore3);
        playerContainers[2] = findViewById(R.id.player3_card);

        // Hạng 4 và 5
        ivAvatars[3] = findViewById(R.id.ivPlayerAvatar4);
        tvPlayerNames[3] = findViewById(R.id.tvPlayerName4);
        tvScores[3] = findViewById(R.id.tvScore4);
        playerContainers[3] = findViewById(R.id.player4_container);

        ivAvatars[4] = findViewById(R.id.ivPlayerAvatar5);
        tvPlayerNames[4] = findViewById(R.id.tvPlayerName5);
        tvScores[4] = findViewById(R.id.tvScore5);
        playerContainers[4] = findViewById(R.id.player5_container);

        // ListView cho các hạng còn lại
        lvOtherPlayers = findViewById(R.id.lvOtherPlayers);
    }

    /**
     * Điền dữ liệu vào UI (Đã cập nhật)
     */
    private void populateLeaderboard(List<PlayerScore> sortedPlayers) {
        if (sortedPlayers == null || sortedPlayers.isEmpty()) {
            Log.d(TAG, "Không có dữ liệu người chơi.");
            return;
        }

        // 1. Điền dữ liệu cho Top 5
        for (int i = 0; i < 5; i++) {
            if (i < sortedPlayers.size()) {
                // Nếu có người chơi ở vị trí này
                PlayerScore player = sortedPlayers.get(i);
                playerContainers[i].setVisibility(View.VISIBLE); // Hiển thị card
                tvPlayerNames[i].setText(player.getUsername());
                tvScores[i].setText(String.valueOf(player.getScore()) + " pts");

                // --- SỬA LỖI: Thay thế Glide bằng Base64 decode ---
                String base64String = player.getPictureBase64();
                Bitmap bitmap = ImageUtils.base64ToBitmap(base64String);

                if (bitmap != null) {
                    ivAvatars[i].setImageBitmap(bitmap);
                } else {
                    ivAvatars[i].setImageResource(R.drawable.ic_default_avatar);
                }
                // --- KẾT THÚC SỬA LỖI ---

            } else {
                // Ẩn các vị trí Top 5 nếu không có đủ người chơi
                playerContainers[i].setVisibility(View.GONE);
            }
        }

        // 2. Điền dữ liệu cho "Other Players" (từ vị trí 6)
        otherPlayersList.clear();
        if (sortedPlayers.size() > 5) {
            otherPlayersList.addAll(sortedPlayers.subList(5, sortedPlayers.size()));
        }

        otherPlayersAdapter.notifyDataSetChanged();
        Log.d(TAG, "Đã cập nhật Bảng xếp hạng.");
    }
}
