package com.ganshapebattle.admin;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddEditPlayerActivity extends AppCompatActivity {

    private TextView tvTitle;
    private Spinner spinnerUser, spinnerLobby, spinnerPicture;
    private EditText etPoint;
    private Button btnSave;

    private PlayerService playerService;
    private UserService userService;
    private LobbyService lobbyService;
    private PictureService pictureService;

    private List<User> userList = new ArrayList<>();
    private List<Lobby> lobbyList = new ArrayList<>();
    private List<Picture> pictureList = new ArrayList<>();

    private String currentUsername, currentLobbyId;
    private Player playerToEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_player);

        tvTitle = findViewById(R.id.tvPlayerTitle);
        etPoint = findViewById(R.id.etPlayerPoint);
        btnSave = findViewById(R.id.btnSavePlayer);

        playerService = new PlayerService();
        userService = new UserService();
        lobbyService = new LobbyService();
        pictureService = new PictureService();

        loadUsers();
        loadLobbies();
        loadPictures();

        currentUsername = getIntent().getStringExtra("PLAYER_USERNAME_EDIT");
        currentLobbyId = getIntent().getStringExtra("PLAYER_LOBBY_ID_EDIT");

        if (currentUsername != null && currentLobbyId != null) {
            tvTitle.setText("Chỉnh sửa điểm");
            spinnerUser.setEnabled(false);
            spinnerLobby.setEnabled(false);
            loadPlayerDetails(currentUsername, currentLobbyId);
        } else {
            tvTitle.setText("Chấm điểm người chơi");
        }

        btnSave.setOnClickListener(v -> savePlayer());
    }

    private void loadUsers() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList = users;
                List<String> usernames = userList.stream().map(User::getUsername).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPlayerActivity.this, android.R.layout.simple_spinner_item, usernames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUser.setAdapter(adapter);
                    if (playerToEdit != null) {
                        setSpinnerSelection(spinnerUser, usernames, playerToEdit.getUsername());
                    }
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void loadLobbies() {
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> lobbies) {
                lobbyList = lobbies;
                List<String> lobbyIds = lobbyList.stream().map(Lobby::getId).collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPlayerActivity.this, android.R.layout.simple_spinner_item, lobbyIds);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerLobby.setAdapter(adapter);
                    if (playerToEdit != null) {
                        setSpinnerSelection(spinnerLobby, lobbyIds, playerToEdit.getLobbyId());
                    }
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void loadPictures() {
        pictureService.getAllPictures(new SupabaseCallback<List<Picture>>() {
            @Override
            public void onSuccess(List<Picture> pictures) {
                pictureList = pictures;
                List<String> pictureInfo = pictureList.stream().map(p -> p.getName() + " (" + p.getId().substring(0, 6) + ")").collect(Collectors.toList());
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddEditPlayerActivity.this, android.R.layout.simple_spinner_item, pictureInfo);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPicture.setAdapter(adapter);
                    if (playerToEdit != null) {
                        setPictureSpinnerSelection();
                    }
                });
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void loadPlayerDetails(String username, String lobbyId) {
        playerService.getPlayerByIds(username, lobbyId, new SupabaseCallback<Player>() {
            @Override
            public void onSuccess(Player player) {
                if (player != null) {
                    playerToEdit = player;
                    runOnUiThread(() -> {
                        etPoint.setText(String.valueOf(player.getPoint()));
                        loadUsers();
                        loadLobbies();
                        loadPictures();
                    });
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void savePlayer() {
        if (spinnerUser.getSelectedItem() == null || spinnerLobby.getSelectedItem() == null) {
            Toast.makeText(this, "Vui lòng chọn User và Lobby", Toast.LENGTH_SHORT).show();
            return;
        }

        Player playerToSave = (playerToEdit == null) ? new Player() : playerToEdit;

        try {
            String selectedUsername = userList.get(spinnerUser.getSelectedItemPosition()).getUsername();
            String selectedLobbyId = lobbyList.get(spinnerLobby.getSelectedItemPosition()).getId();
            String selectedPictureId = pictureList.isEmpty() ? null : pictureList.get(spinnerPicture.getSelectedItemPosition()).getId();

            if (playerToEdit == null) {
                playerToSave.setUsername(selectedUsername);
                playerToSave.setLobbyId(selectedLobbyId);
            }

            playerToSave.setPictureId(selectedPictureId);
            playerToSave.setPoint(Integer.parseInt(etPoint.getText().toString()));
            // Hạng sẽ được tính riêng nên không set ở đây
            // playerToSave.setRank(...);
        } catch (Exception e) {
            Toast.makeText(this, "Dữ liệu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        executeSaveOrUpdate(playerToSave);
    }

    private void executeSaveOrUpdate(Player player) {
        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditPlayerActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("AddEditPlayer", "Lỗi: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditPlayerActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        };

        if (playerToEdit == null) {
            playerService.insertPlayer(player, callback);
        } else {
            playerService.updatePlayer(currentUsername, currentLobbyId, player, callback);
        }
    }

    private void setSpinnerSelection(Spinner spinner, List<String> options, String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setPictureSpinnerSelection() {
        if (playerToEdit == null || pictureList.isEmpty()) return;
        for (int i = 0; i < pictureList.size(); i++) {
            if (pictureList.get(i).getId().equals(playerToEdit.getPictureId())) {
                spinnerPicture.setSelection(i);
                return;
            }
        }
    }
}