package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerCRUDActivity extends AppCompatActivity {

    private ListView lvPlayers;
    private Button btnAddPlayer;
    private SearchView searchView;

    // Thêm LobbyService
    private PlayerService playerService;
    private LobbyService lobbyService;

    private ArrayAdapter<String> adapter;

    private List<Player> displayedPlayerList = new ArrayList<>();
    private List<Player> fullPlayerList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditPlayerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_crud);

        lvPlayers = findViewById(R.id.lvPlayers);
        btnAddPlayer = findViewById(R.id.btnAddPlayer);
        searchView = findViewById(R.id.searchViewPlayers);

        // Khởi tạo cả hai service
        playerService = new PlayerService();
        lobbyService = new LobbyService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvPlayers.setAdapter(adapter);

        loadPlayersAndLobbies(); // Đổi tên hàm để phản ánh logic mới
        setupSearch();

        lvPlayers.setOnItemClickListener((parent, view, position, id) -> {
            Player selectedPlayer = displayedPlayerList.get(position);
            Intent intent = new Intent(this, PlayerDetailActivity.class);
            intent.putExtra("PLAYER_USERNAME", selectedPlayer.getUsername());
            intent.putExtra("PLAYER_LOBBY_ID", selectedPlayer.getLobbyId());
            startActivity(intent);
        });

        btnAddPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditPlayerActivity.class);
            addEditPlayerLauncher.launch(intent);
        });

        addEditPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadPlayersAndLobbies();
                    }
                }
        );
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPlayers(newText);
                return true;
            }
        });
    }

    private void loadPlayersAndLobbies() {
        // Bước 1: Lấy danh sách tất cả Players
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                // Bước 2: Lấy danh sách tất cả Lobbies
                lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
                    @Override
                    public void onSuccess(List<Lobby> lobbies) {
                        // Bước 3: Xử lý và kết hợp dữ liệu
                        runOnUiThread(() -> {
                            // Sắp xếp danh sách người chơi: ưu tiên theo lobby, sau đó theo hạng
                            players.sort(Comparator.comparing(Player::getLobbyId)
                                    .thenComparingInt(Player::getRank));

                            fullPlayerList.clear();
                            fullPlayerList.addAll(players);

                            // Tạo một Map để tra cứu thông tin lobby nhanh hơn
                            Map<String, Lobby> lobbyMap = lobbies.stream()
                                    .collect(Collectors.toMap(Lobby::getId, lobby -> lobby));

                            updateDisplayedPlayers(fullPlayerList, lobbyMap);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("PlayerCRUD", "Lỗi tải lobbies: ", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("PlayerCRUD", "Lỗi tải players: ", e);
            }
        });
    }

    private void filterPlayers(String query) {
        // Tạm thời vô hiệu hóa bộ lọc để tập trung vào hiển thị, bạn có thể thêm lại sau
        // Hoặc cập nhật logic filter để tìm kiếm trên chuỗi đã định dạng
    }

    // Trong file PlayerCRUDActivity.java

    private void updateDisplayedPlayers(List<Player> players, Map<String, Lobby> lobbyMap) {
        displayedPlayerList.clear();
        displayedPlayerList.addAll(players);

        List<String> playerInfo = displayedPlayerList.stream()
                .map(player -> {
                    Lobby lobby = lobbyMap.get(player.getLobbyId());
                    String lobbyIdentifier = (lobby != null) ? lobby.getMode() : "Unknown Lobby";

                    // Bắt đầu thay đổi logic hiển thị ở đây
                    String rankText;
                    if (player.getRank() == 0) {
                        // Nếu hạng là 0, hiển thị lời nhắc
                        rankText = player.getUsername() + "\n(Vào lobby để cập nhật hạng)";
                    } else {
                        // Nếu đã có hạng, hiển thị đầy đủ thông tin
                        rankText = "Hạng " + player.getRank() + " | " + player.getUsername();
                    }

                    return rankText + "\n(Lobby: " + lobbyIdentifier + " | Điểm: " + player.getPoint() + ")";
                    // Kết thúc thay đổi
                })
                .collect(Collectors.toList());

        adapter.clear();
        adapter.addAll(playerInfo);
        adapter.notifyDataSetChanged();
    }
}