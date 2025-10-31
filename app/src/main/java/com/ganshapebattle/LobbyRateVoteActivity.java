package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// SỬA LỖI: Import đúng Activity
import com.ganshapebattle.admin.AddEditPlayerActivity;
// import com.ganshapebattle.admin.PlayerDetailActivity; // Không dùng cái này nữa
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LobbyRateVoteActivity extends AppCompatActivity {

    private static final String TAG = "LobbyRateVoteActivity";

    // --- UI Elements ---
    private ListView lvPlayers;
    private Button btnFinish;
    private SearchView searchViewPlayers;

    // --- Services & Data ---
    private PlayerService playerService;
    private LobbyService lobbyService;
    private ArrayAdapter<String> adapter;

    private final List<Player> fullPlayerList = new ArrayList<>();
    private final List<Player> displayedPlayerList = new ArrayList<>();

    // --- Thông tin phòng hiện tại ---
    private String lobbyId;
    private String username;
    private Lobby currentLobby;

    // --- Activity Launcher ---
    // SỬA LỖI: Đổi tên launcher cho rõ nghĩa
    private ActivityResultLauncher<Intent> addEditLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lobby_rate_vote);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy thông tin từ Intent
        lobbyId = getIntent().getStringExtra("lobbyid");
        username = getIntent().getStringExtra("username");

        if (lobbyId == null || username == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin Lobby hoặc User.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Khởi tạo services
        playerService = new PlayerService();
        lobbyService = new LobbyService();

        bindViews();
        setupListeners();

        // Thiết lập Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvPlayers.setAdapter(adapter);

        // Khởi tạo launcher
        // SỬA LỖI: Đổi tên launcher
        addEditLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Quay lại từ AddEdit, tải lại dữ liệu...");
                        loadData(); // Tải lại danh sách
                    }
                });

        Log.d(TAG, "onCreate: Activity được tạo.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Tải lại dữ liệu...");
        loadData(); // Tải dữ liệu khi quay lại
    }

    /**
     * Ánh xạ các thành phần UI
     */
    private void bindViews() {
        lvPlayers = findViewById(R.id.lvPlayers);
        btnFinish = findViewById(R.id.btnFinish);
        searchViewPlayers = findViewById(R.id.searchViewPlayers);
    }

    /**
     * Thiết lập các sự kiện click và search
     */
    private void setupListeners() {
        // Sự kiện Click cho nút Finish
        btnFinish.setOnClickListener(v -> handleFinishClick());

        // Sự kiện click cho ListView
        lvPlayers.setOnItemClickListener((parent, view, position, id) -> {
            Player selectedPlayer = displayedPlayerList.get(position);
            Log.d(TAG, "Đã chọn player: " + selectedPlayer.getUsername());

            // --- SỬA LỖI TRUYỀN DỮ LIỆU ---
            // 1. Chỉ Admin mới được quyền chấm điểm (edit)
            boolean isAdmin = (currentLobby != null) && username.equals(currentLobby.getAdminUsername());
            if (!isAdmin) {
                Toast.makeText(this, "Chỉ Admin mới có quyền chấm điểm.", Toast.LENGTH_SHORT).show();
                // Bạn có thể mở PlayerDetailActivity (chỉ xem) ở đây nếu muốn
                return;
            }

            // 2. Mở AddEditPlayerActivity
            Intent intent = new Intent(LobbyRateVoteActivity.this, AddEditPlayerActivity.class);

            // 3. Truyền đúng 2 key mà AddEditPlayerActivity mong đợi
            intent.putExtra("PLAYER_USERNAME", selectedPlayer.getUsername());
            intent.putExtra("LOBBY_ID", selectedPlayer.getLobbyId());

            // 4. Khởi chạy
            addEditLauncher.launch(intent);
        });

        // Sự kiện Search
        searchViewPlayers.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterPlayers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPlayers(newText);
                return false;
            }
        });
    }

    /**
     * Tải dữ liệu (Lobby và Players)
     */
    private void loadData() {
        if (lobbyId == null) return;
        Toast.makeText(this, "Đang tải dữ liệu phòng...", Toast.LENGTH_SHORT).show();

        // Bước 1: Lấy thông tin Lobby (để biết admin là ai)
        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby == null) {
                    runOnUiThread(() -> Toast.makeText(LobbyRateVoteActivity.this, "Lỗi: Không tìm thấy Lobby", Toast.LENGTH_LONG).show());
                    finish();
                    return;
                }
                currentLobby = lobby; // Lưu lại thông tin lobby

                // Ẩn nút Finish nếu không phải là Admin
                runOnUiThread(() -> {
                    if (!username.equals(currentLobby.getAdminUsername())) {
                        btnFinish.setVisibility(View.GONE);
                    } else {
                        btnFinish.setVisibility(View.VISIBLE);
                    }
                });

                // Bước 2: Tải tất cả player (sau đó lọc)
                playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
                    @Override
                    public void onSuccess(List<Player> allPlayers) {
                        // Lọc ra chỉ những players thuộc lobby này
                        List<Player> lobbyPlayers = allPlayers.stream()
                                .filter(player -> lobbyId.equals(player.getLobbyId()))
                                // Sắp xếp theo điểm, cao nhất lên trước
                                .sorted(Comparator.comparingInt(Player::getPoint).reversed())
                                .collect(Collectors.toList());

                        Log.d(TAG, "Đã tải " + lobbyPlayers.size() + " players cho lobby " + lobbyId);

                        runOnUiThread(() -> {
                            fullPlayerList.clear();
                            fullPlayerList.addAll(lobbyPlayers);
                            updateDisplayedPlayers(fullPlayerList); // Hiển thị
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Lỗi tải players: ", e);
                        runOnUiThread(() -> Toast.makeText(LobbyRateVoteActivity.this, "Lỗi tải danh sách players: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải lobby: ", e);
                runOnUiThread(() -> Toast.makeText(LobbyRateVoteActivity.this, "Lỗi tải thông tin lobby: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Lọc danh sách player theo ô search
     */
    private void filterPlayers(String query) {
        List<Player> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(fullPlayerList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = fullPlayerList.stream()
                    .filter(player -> player.getUsername() != null && player.getUsername().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateDisplayedPlayers(filteredList);
    }

    /**
     * Cập nhật ListView với danh sách Player (đã được lọc)
     */
    private void updateDisplayedPlayers(List<Player> players) {
        displayedPlayerList.clear();
        displayedPlayerList.addAll(players);

        // Tạo danh sách String để hiển thị
        List<String> playerInfo = new ArrayList<>();
        for (int i = 0; i < displayedPlayerList.size(); i++) {
            Player player = displayedPlayerList.get(i);

            // Gán rank dựa trên thứ tự đã sắp xếp
            player.setRank(i + 1);

            String rankText = "Hạng " + player.getRank();
            String info = rankText + " | " + player.getUsername() + "\nĐiểm: " + player.getPoint();
            playerInfo.add(info);
        }

        adapter.clear();
        adapter.addAll(playerInfo);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter players đã cập nhật.");
    }

    /**
     * Xử lý sự kiện nhấn nút Finish (Yêu cầu mới)
     */
    private void handleFinishClick() {
        if (currentLobby == null) {
            Toast.makeText(this, "Chưa tải được thông tin phòng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra lại quyền Admin
        if (!username.equals(currentLobby.getAdminUsername())) {
            Toast.makeText(this, "Chỉ Admin mới có thể kết thúc trò chơi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị Dialog xác nhận
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kết thúc trò chơi")
                .setMessage("Bạn có chắc chắn muốn kết thúc trò chơi và xem kết quả?")
                .setPositiveButton("Kết thúc", (dialog, which) -> {
                    // Update status của Lobby
                    updateLobbyStatusToIsEnd();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Cập nhật status của Lobby lên "isEnd"
     */
    private void updateLobbyStatusToIsEnd() {
        Toast.makeText(this, "Đang kết thúc trò chơi...", Toast.LENGTH_SHORT).show();

        currentLobby.setStatus("isEnd"); // <-- Yêu cầu của bạn

        lobbyService.updateLobby(currentLobby.getId(), currentLobby, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(LobbyRateVoteActivity.this, "Đã kết thúc! Đang xem kết quả.", Toast.LENGTH_SHORT).show();

                    // Mở GameEndActivity
                    Intent intent = new Intent(LobbyRateVoteActivity.this, GameEndActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("lobbyid", lobbyId);
                    startActivity(intent);
                    finish(); // Đóng Activity này
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi cập nhật status: ", e);
                runOnUiThread(() -> Toast.makeText(LobbyRateVoteActivity.this, "Lỗi khi kết thúc: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}