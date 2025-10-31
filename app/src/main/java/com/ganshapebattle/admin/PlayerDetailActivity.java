package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

public class PlayerDetailActivity extends AppCompatActivity {

    private TextView tvUsername, tvLobbyId, tvPoint, tvRank, tvPictureId;
    private Button btnUpdate, btnDelete;
    private PlayerService playerService;
    private String currentUsername, currentLobbyId;
    private ActivityResultLauncher<Intent> editPlayerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_detail);

        tvUsername = findViewById(R.id.tvPlayerUsername);
        tvLobbyId = findViewById(R.id.tvPlayerLobbyId);
        tvPoint = findViewById(R.id.tvPlayerPoint);
        tvRank = findViewById(R.id.tvPlayerRank);
        tvPictureId = findViewById(R.id.tvPlayerPictureId);
        btnUpdate = findViewById(R.id.btnUpdatePlayer);
        btnDelete = findViewById(R.id.btnDeletePlayer);

        playerService = new PlayerService();
        currentUsername = getIntent().getStringExtra("PLAYER_USERNAME");
        currentLobbyId = getIntent().getStringExtra("PLAYER_LOBBY_ID");

        if (currentUsername != null && currentLobbyId != null) {
            fetchPlayerDetails(currentUsername, currentLobbyId);
        }

        btnUpdate.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditPlayerActivity.class);
            intent.putExtra("PLAYER_USERNAME_EDIT", currentUsername);
            intent.putExtra("PLAYER_LOBBY_ID_EDIT", currentLobbyId);
            editPlayerLauncher.launch(intent);
        });

        btnDelete.setOnClickListener(v -> deletePlayer(currentUsername, currentLobbyId));

        editPlayerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fetchPlayerDetails(currentUsername, currentLobbyId);
                    }
                }
        );
    }

    private void fetchPlayerDetails(String username, String lobbyId) {
        playerService.getPlayerByIds(username, lobbyId, new SupabaseCallback<Player>() {
            @Override
            public void onSuccess(Player player) {
                runOnUiThread(() -> {
                    if (player != null) {
                        tvUsername.setText(player.getUsername());
                        tvLobbyId.setText(player.getLobbyId());
                        tvPoint.setText(String.valueOf(player.getPoint()));
                        tvRank.setText(String.valueOf(player.getRank()));
                        tvPictureId.setText(player.getPictureId());
                    }
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }

    private void deletePlayer(String username, String lobbyId) {
        playerService.deletePlayer(username, lobbyId, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
//                    Toast.makeText(PlayerDetailActivity.this, result, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) { /* Handle error */ }
        });
    }
}