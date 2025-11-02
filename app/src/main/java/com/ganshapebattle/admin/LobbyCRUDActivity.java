// File: main/java/com/ganshapebattle/admin/LobbyCRUDActivity.java
package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
//import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// === SỬA LỖI: THAY ĐỔI IMPORT ===
import androidx.appcompat.widget.SearchView;
// ================================

import com.ganshapebattle.R;
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
    // === SỬA LỖI: THAY ĐỔI KIỂU BIẾN ===
    private androidx.appcompat.widget.SearchView searchView;
    // ==================================
    private LobbyService lobbyService;
    private ArrayAdapter<String> adapter;

    private final List<Lobby> displayedLobbyList = new ArrayList<>();
    private final List<Lobby> fullLobbyList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditLobbyLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_crud);

        lvLobbies = findViewById(R.id.lvLobbies);
        // === SỬA LỖI: ID của nút trong XML là 'fabAddLobby' ===
        btnAddLobby = findViewById(R.id.fabAddLobby);
        // ===================================================
        searchView = findViewById(R.id.searchViewLobbies);
        lobbyService = new LobbyService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvLobbies.setAdapter(adapter);

        setupSearch();

        // Khởi tạo Launcher
        addEditLobbyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Nhận được kết quả OK từ AddEditLobbyActivity/LobbyDetailActivity.");
                    } else {
                        Log.d(TAG, "AddEditLobbyActivity/LobbyDetailActivity không trả về RESULT_OK.");
                    }
                }
        );

        // Mở màn hình CHI TIẾT khi nhấn vào item
        lvLobbies.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < displayedLobbyList.size()) {
                Lobby selectedLobby = displayedLobbyList.get(position);
                Intent intent = new Intent(LobbyCRUDActivity.this, LobbyDetailActivity.class);
                intent.putExtra("LOBBY_ID", selectedLobby.getId());
                startActivity(intent);
            }
        });

        // Mở màn hình THÊM MỚI khi nhấn nút Add
        btnAddLobby.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyCRUDActivity.this, AddEditLobbyActivity.class);
            addEditLobbyLauncher.launch(intent);
        });

    } // Kết thúc onCreate

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume được gọi, tải lại danh sách lobbies.");
        loadLobbies();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) { filterLobbies(newText); return true; }
        });
        searchView.setOnCloseListener(() -> { filterLobbies(""); return false; });
    }

    private void loadLobbies() {
        Log.d(TAG, "Bắt đầu tải danh sách lobbies...");
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> result) {
                Log.d(TAG, "Tải lobbies thành công: " + (result != null ? result.size() : 0));
                runOnUiThread(() -> {
                    fullLobbyList.clear();
                    if (result != null) { fullLobbyList.addAll(result); }
                    filterLobbies(searchView.getQuery().toString());
                });
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải lobbies: ", e);
//                runOnUiThread(() -> Toast.makeText(LobbyCRUDActivity.this, "Lỗi tải lobbies", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterLobbies(String query) {
        List<Lobby> filteredList;
        if (query == null || query.isEmpty()) {
            filteredList = new ArrayList<>(fullLobbyList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            filteredList = fullLobbyList.stream()
                    .filter(lobby -> (lobby.getId() != null && lobby.getId().toLowerCase().contains(lowerCaseQuery)) ||
                            (lobby.getMode() != null && lobby.getMode().toLowerCase().contains(lowerCaseQuery)) )
                    .collect(Collectors.toList());
        }
        updateDisplayedLobbies(filteredList);
    }

    private void updateDisplayedLobbies(List<Lobby> lobbies) {
        displayedLobbyList.clear();
        displayedLobbyList.addAll(lobbies);
        List<String> lobbyInfo = displayedLobbyList.stream()
                .map(lobby -> "ID: " + lobby.getId() + " | Mode: " + (lobby.getMode() != null ? lobby.getMode() : "N/A"))
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(lobbyInfo);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "Adapter lobbies đã cập nhật.");
    }
}