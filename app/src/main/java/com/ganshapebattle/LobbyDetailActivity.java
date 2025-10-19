package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
                    Toast.makeText(LobbyDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }

    private void calculateAndApplyRanks(String lobbyId) {
        Toast.makeText(this, "Đang tính toán xếp hạng...", Toast.LENGTH_SHORT).show();

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
                    runOnUiThread(() -> Toast.makeText(LobbyDetailActivity.this, "Không có người chơi nào trong phòng này.", Toast.LENGTH_SHORT).show());
                    return;
                }

                for (int i = 0; i < totalPlayers; i++) {
                    Player player = playersInLobby.get(i);
                    player.setRank(i + 1);

                    playerService.updatePlayer(player.getUsername(), player.getLobbyId(), player, new SupabaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Log.d(TAG, "Cập nhật hạng thành công cho: " + player.getUsername());
                            successCount[0]++;
                            if (successCount[0] == totalPlayers) {
                                runOnUiThread(() -> Toast.makeText(LobbyDetailActivity.this, "Cập nhật xếp hạng hoàn tất!", Toast.LENGTH_LONG).show());
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Lỗi cập nhật hạng cho: " + player.getUsername(), e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> Toast.makeText(LobbyDetailActivity.this, "Lỗi khi lấy danh sách người chơi.", Toast.LENGTH_SHORT).show());
            }
        });
    }
}