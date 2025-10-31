package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LobbyDetailActivity extends AppCompatActivity {

    private static final String TAG = "LobbyDetailActivity";

    private TextView tvLobbyId, tvLobbyAdmin, tvLobbyMode, tvLobbyStatus, tvLobbyMaxPlayers;
    private Button btnUpdateLobby, btnDeleteLobby, btnCalculateRanks;
    private LobbyService lobbyService;
    private PlayerService playerService;
    private String currentLobbyId;
    private ActivityResultLauncher<Intent> editLobbyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_detail);

        tvLobbyId = findViewById(R.id.tvLobbyId);
        tvLobbyAdmin = findViewById(R.id.tvLobbyAdmin);
        tvLobbyMode = findViewById(R.id.tvLobbyMode);
        tvLobbyStatus = findViewById(R.id.tvLobbyStatus);
        tvLobbyMaxPlayers = findViewById(R.id.tvLobbyMaxPlayers);
        btnUpdateLobby = findViewById(R.id.btnUpdateLobby);
        btnDeleteLobby = findViewById(R.id.btnDeleteLobby);
        btnCalculateRanks = findViewById(R.id.btnCalculateRanks);

        lobbyService = new LobbyService();
        playerService = new PlayerService();
        currentLobbyId = getIntent().getStringExtra("LOBBY_ID");

        if (currentLobbyId != null) {
            fetchLobbyDetails(currentLobbyId);
        } else {
            finish();
        }

        btnUpdateLobby.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyDetailActivity.this, AddEditLobbyActivity.class);
            intent.putExtra("LOBBY_ID_EDIT", currentLobbyId);
            editLobbyLauncher.launch(intent);
        });

        btnDeleteLobby.setOnClickListener(v -> deleteLobby(currentLobbyId));

        btnCalculateRanks.setOnClickListener(v -> {
            if (currentLobbyId != null) {
                calculateAndApplyRanks(currentLobbyId);
            }
        });

        editLobbyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchLobbyDetails(currentLobbyId);
                    }
                }
        );
    }

    /** Helper để quản lý trạng thái Loading của nút tính toán Rank */
    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                btnCalculateRanks.setEnabled(false);
                btnCalculateRanks.setText("Đang tính toán...");
            } else {
                btnCalculateRanks.setEnabled(true);
                btnCalculateRanks.setText(getString(R.string.calculate_ranks_button));
            }
        });
    }

    private void fetchLobbyDetails(String lobbyId) {
        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                runOnUiThread(() -> {
                    if (lobby != null) {
                        tvLobbyId.setText(lobby.getId());
                        tvLobbyAdmin.setText(lobby.getAdminUsername());
                        tvLobbyMode.setText(lobby.getMode());
                        tvLobbyStatus.setText(lobby.getStatus());
                        tvLobbyMaxPlayers.setText(String.valueOf(lobby.getMaxPlayer()));
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching lobby details: ", e);
            }
        });
    }

    private void deleteLobby(String lobbyId) {
        lobbyService.deleteLobby(lobbyId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
//                    Toast.makeText(LobbyDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }

    private void calculateAndApplyRanks(String lobbyId) {
        setLoadingState(true); // Bắt đầu loading

        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                List<Player> playersInLobby = allPlayers.stream()
                        .filter(p -> p.getLobbyId().equals(lobbyId))
                        .collect(Collectors.toList());

                playersInLobby.sort(Comparator.comparingInt(Player::getPoint).reversed());

                final int[] successCount = {0};
                final int totalPlayers = playersInLobby.size();

                if (totalPlayers == 0) {
                    runOnUiThread(() -> {
                        setLoadingState(false);
//                        Toast.makeText(LobbyDetailActivity.this, "Không có người chơi nào trong phòng này.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Bắt đầu cập nhật Player đầu tiên
                updatePlayerRank(playersInLobby, 0, totalPlayers);
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    setLoadingState(false);
//                    Toast.makeText(LobbyDetailActivity.this, "Lỗi khi lấy danh sách người chơi.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** Recursive function để cập nhật rank từng Player */
    private void updatePlayerRank(List<Player> players, int index, int totalPlayers) {
        if (index >= totalPlayers) {
            // Hoàn thành tất cả các lần cập nhật
            runOnUiThread(() -> {
                setLoadingState(false);
//                Toast.makeText(LobbyDetailActivity.this, "Cập nhật xếp hạng hoàn tất!", Toast.LENGTH_LONG).show();
            });
            return;
        }

        Player player = players.get(index);
        player.setRank(index + 1); // Gán rank mới

        playerService.updatePlayer(player.getUsername(), player.getLobbyId(), player, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Tiếp tục cập nhật Player tiếp theo (dùng đệ quy)
                updatePlayerRank(players, index + 1, totalPlayers);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi cập nhật hạng cho: " + player.getUsername(), e);
                // Vẫn tiếp tục cập nhật Player tiếp theo dù có lỗi
                updatePlayerRank(players, index + 1, totalPlayers);
            }
        });
    }
}