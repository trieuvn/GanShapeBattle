package com.ganshapebattle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LobbyCRUDActivity extends AppCompatActivity {

    private static final String TAG = "LobbyCRUDActivity";

    private ListView lvLobbies;
    private Button btnAddLobby;
    private SearchView searchView;
    private LobbyService lobbyService;
    private ArrayAdapter<String> adapter;

    private List<Lobby> displayedLobbyList = new ArrayList<>();
    private List<Lobby> fullLobbyList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditLobbyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_crud);

        lvLobbies = findViewById(R.id.lvLobbies);
        btnAddLobby = findViewById(R.id.btnAddLobby);
        searchView = findViewById(R.id.searchViewLobbies);
        lobbyService = new LobbyService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvLobbies.setAdapter(adapter);

        loadLobbies();
        setupSearch();

        lvLobbies.setOnItemClickListener((parent, view, position, id) -> {
            Lobby selectedLobby = displayedLobbyList.get(position);
            Intent intent = new Intent(LobbyCRUDActivity.this, LobbyDetailActivity.class);
            intent.putExtra("LOBBY_ID", selectedLobby.getId());
            startActivity(intent);
        });

        btnAddLobby.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyCRUDActivity.this, AddEditLobbyActivity.class);
            addEditLobbyLauncher.launch(intent);
        });

        addEditLobbyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadLobbies();
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
                filterLobbies(newText);
                return true;
            }
        });
    }

    private void loadLobbies() {
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> result) {
                runOnUiThread(() -> {
                    fullLobbyList.clear();
                    fullLobbyList.addAll(result);
                    updateDisplayedLobbies(fullLobbyList);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading lobbies: ", e);
            }
        });
    }

    private void filterLobbies(String query) {
        if (query == null || query.isEmpty()) {
            updateDisplayedLobbies(fullLobbyList);
        } else {
            List<Lobby> filteredList = fullLobbyList.stream()
                    .filter(lobby -> lobby.getId().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            updateDisplayedLobbies(filteredList);
        }
    }

    private void updateDisplayedLobbies(List<Lobby> lobbies) {
        displayedLobbyList.clear();
        displayedLobbyList.addAll(lobbies);
        // Hiển thị ID và Mode của Lobby để dễ nhận biết
        List<String> lobbyInfo = displayedLobbyList.stream()
                .map(lobby -> "ID: " + lobby.getId() + " | Mode: " + lobby.getMode())
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(lobbyInfo);
        adapter.notifyDataSetChanged();
    }
}