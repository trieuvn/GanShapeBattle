// File: main/java/com/ganshapebattle/admin/PlayerCRUDActivity.java
package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast; // Thêm Toast

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R; //
import com.ganshapebattle.models.Lobby; //
import com.ganshapebattle.models.Player; //
import com.ganshapebattle.services.LobbyService; //
import com.ganshapebattle.services.PlayerService; //
import com.ganshapebattle.services.SupabaseCallback; //

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap; // Thêm HashMap
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerCRUDActivity extends AppCompatActivity {

    private static final String TAG = "PlayerCRUDActivity"; // Thêm TAG

    private ListView lvPlayers;
    private Button btnAddPlayer;
    private SearchView searchView;

    private PlayerService playerService;
    private LobbyService lobbyService;

    private ArrayAdapter<String> adapter;

    private final List<Player> displayedPlayerList = new ArrayList<>();
    private final List<Player> fullPlayerList = new ArrayList<>();
    private final Map<String, Lobby> lobbyMap = new HashMap<>();

    private ActivityResultLauncher<Intent> addEditPlayerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_crud); //

        lvPlayers = findViewById(R.id.lvPlayers); //
        btnAddPlayer = findViewById(R.id.btnAddPlayer); //
        searchView = findViewById(R.id.searchViewPlayers); //

        playerService = new PlayerService(); //
        lobbyService = new LobbyService(); //

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvPlayers.setAdapter(adapter);

        setupSearch();

        // Khởi tạo Launcher
        addEditPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Nhận được kết quả OK từ AddEditPlayerActivity/PlayerDetailActivity.");
                        // onResume sẽ tự động gọi loadPlayersAndLobbies()
                    } else {
                        Log.d(TAG, "AddEditPlayerActivity/PlayerDetailActivity không trả về RESULT_OK.");
                    }
                }
        );

        // Mở màn hình CHI TIẾT khi nhấn vào item
        lvPlayers.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayedPlayerList.size()) {
                Player selectedPlayer = displayedPlayerList.get(position);
                Intent intent = new Intent(this, PlayerDetailActivity.class); // <<< Mở PlayerDetailActivity
                // Truyền cả username và lobbyId để xác định Player
                intent.putExtra("PLAYER_USERNAME", selectedPlayer.getUsername());
                intent.putExtra("PLAYER_LOBBY_ID", selectedPlayer.getLobbyId());
                startActivity(intent); // <<< Dùng startActivity thông thường
                // Lưu ý: PlayerDetailActivity cần dùng launcher để mở AddEditPlayerActivity
            }
        });

        // Mở màn hình THÊM MỚI/CHẤM ĐIỂM khi nhấn nút Add
        btnAddPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditPlayerActivity.class); //
            addEditPlayerLauncher.launch(intent); // Dùng launcher
        });

    } // Kết thúc onCreate

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại danh sách players và lobbies.");
        loadPlayersAndLobbies(); // Tải lại dữ liệu khi quay lại màn hình
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterPlayers(newText); return true; }
        });
        searchView.setOnCloseListener(() -> { filterPlayers(""); return false; });
    }

    private void loadPlayersAndLobbies() {
        Log.d(TAG, "Bắt đầu tải danh sách players và lobbies...");
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() { //
            @Override
            public void onSuccess(List<Lobby> lobbies) {
                Log.d(TAG, "Tải lobbies thành công: " + (lobbies != null ? lobbies.size() : 0));
                lobbyMap.clear();
                if (lobbies != null) {
                    for (Lobby lobby : lobbies) { lobbyMap.put(lobby.getId(), lobby); }
                }
                playerService.getAllPlayers(new SupabaseCallback<List<Player>>() { //
                    @Override
                    public void onSuccess(List<Player> players) {
                        Log.d(TAG, "Tải players thành công: " + (players != null ? players.size() : 0));
                        runOnUiThread(() -> {
                            fullPlayerList.clear();
                            if (players != null) {
                                players.sort(Comparator.comparing(Player::getLobbyId).thenComparingInt(Player::getRank));
                                fullPlayerList.addAll(players);
                            }
                            filterPlayers(searchView.getQuery().toString());
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Lỗi tải players: ", e);
                        runOnUiThread(() -> Toast.makeText(PlayerCRUDActivity.this, "Lỗi tải người chơi", Toast.LENGTH_SHORT).show());
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải lobbies: ", e);
                runOnUiThread(() -> Toast.makeText(PlayerCRUDActivity.this, "Lỗi tải phòng", Toast.LENGTH_SHORT).show());
            }
        });
    }


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

    private void updateDisplayedPlayers(List<Player> players) {
        displayedPlayerList.clear();
        displayedPlayerList.addAll(players);
        List<String> playerInfo = displayedPlayerList.stream()
                .map(player -> {
                    Lobby lobby = lobbyMap.get(player.getLobbyId());
                    String lobbyIdentifier = (lobby != null && lobby.getMode() != null) ? lobby.getMode() : (lobby != null ? lobby.getId().substring(0, 6)+"..." : "???");
                    String rankText = (player.getRank() <= 0) ? player.getUsername() + " (Chưa xếp hạng)" : "Hạng " + player.getRank() + " | " + player.getUsername();
                    return rankText + "\n(Lobby: " + lobbyIdentifier + " | Điểm: " + player.getPoint() + ")";
                })
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(playerInfo);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter players đã cập nhật.");
    }
}