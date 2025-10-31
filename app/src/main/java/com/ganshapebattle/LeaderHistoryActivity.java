package com.ganshapebattle;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.models.Lobby; // THÊM IMPORT
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService; // THÊM IMPORT
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;

public class LeaderHistoryActivity extends AppCompatActivity {

    private static final String TAG = "LeaderHistoryActivity";
    private ListView lvHistory;
    private TextView textViewUserName;
    private ProgressBar progressBarLoading;
    private TextView textViewNoHistory;

    private PlayerService playerService;
    private LobbyService lobbyService;

    private String username;
    private ArrayAdapter<String> adapter;

    private final SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_history);

        // Lấy username từ Intent
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có thông tin người dùng!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // === BINDING ===
        lvHistory = findViewById(R.id.lvHistory);
        textViewUserName = findViewById(R.id.textViewUserName);
        progressBarLoading = findViewById(R.id.progressBarLoading);
        textViewNoHistory = findViewById(R.id.textViewNoHistory);
        // ===========================

        if (textViewUserName != null) {
            TextView tvTitle = findViewById(R.id.tvHistoryTitle);
            if(tvTitle != null) {
                tvTitle.setText("Thống kê Xếp hạng");
            }
            textViewUserName.setText(String.format("Dữ liệu cho: %s", username));
        }

        // Khởi tạo Service và Adapter
        playerService = new PlayerService();
        lobbyService = new LobbyService();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvHistory.setAdapter(adapter);

        // === BẮT ĐẦU CHUỖI CẬP NHẬT RANK VÀ TẢI STATS ===
        calculateRanksForAllFinishedLobbies();
    }

    /**
     * BƯỚC 1: ORCHESTRATION - Tải tất cả lobbies và bắt đầu tính toán xếp hạng
     */
    private void calculateRanksForAllFinishedLobbies() {
        if (progressBarLoading != null) progressBarLoading.setVisibility(View.VISIBLE);

        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> allLobbies) {
                // 2. Lọc ra các phòng đã kết thúc ("isOver" hoặc "isEnd")
                List<Lobby> finishedLobbies = allLobbies.stream()
                        .filter(lobby -> "isOver".equals(lobby.getStatus()) || "isEnd".equals(lobby.getStatus()))
                        .collect(Collectors.toList());

                if (finishedLobbies.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy phòng đã kết thúc. Chuyển sang tải thống kê.");
                    runOnUiThread(() -> loadUserRankStatistics()); // FIX: Chuyển sang UI thread
                    return;
                }

                Log.d(TAG, "Tìm thấy " + finishedLobbies.size() + " phòng cần tính lại rank. Bắt đầu tính toán...");

                // 3. Bắt đầu xử lý từng lobby một (recursive call)
                processNextLobby(finishedLobbies, 0);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải danh sách lobbies. Vẫn tải thống kê.", e);
                runOnUiThread(() -> loadUserRankStatistics()); // FIX: Chuyển sang UI thread
            }
        });
    }

    /**
     * BƯỚC 2: RECURSIVE LOBBY PROCESSING - Xử lý từng lobby
     */
    private void processNextLobby(List<Lobby> lobbies, int index) {
        if (index >= lobbies.size()) {
            Log.d(TAG, "Đã hoàn thành cập nhật rank cho tất cả lobbies. Tải thống kê.");
            runOnUiThread(() -> loadUserRankStatistics()); // FIX: Chạy trên UI Thread
            return;
        }

        Lobby currentLobby = lobbies.get(index);
        Log.d(TAG, "Đang xử lý rank cho lobby: " + currentLobby.getId());

        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                List<Player> playersInLobby = allPlayers.stream()
                        .filter(p -> currentLobby.getId().equals(p.getLobbyId()))
                        .collect(Collectors.toList());

                // 1. Sắp xếp players theo điểm (cao nhất trước)
                playersInLobby.sort(Comparator.comparingInt(Player::getPoint).reversed());

                if (playersInLobby.isEmpty()) {
                    Log.d(TAG, "Lobby " + currentLobby.getId() + " không có người chơi. Bỏ qua.");
                    processNextLobby(lobbies, index + 1);
                    return;
                }

                // 2. Bắt đầu gán và cập nhật Rank cho từng Player
                applyRanksSequentially(playersInLobby, 0, currentLobby.getId(), new Runnable() {
                    @Override
                    public void run() {
                        processNextLobby(lobbies, index + 1); // Sau khi hoàn tất cập nhật tất cả Player, chuyển sang lobby tiếp theo
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi tải players cho lobby " + currentLobby.getId() + ". Bỏ qua.", e);
                processNextLobby(lobbies, index + 1); // Lỗi, chuyển sang lobby tiếp theo
            }
        });
    }

    /**
     * BƯỚC 3: RECURSIVE PLAYER PROCESSING - Áp dụng rank và cập nhật Player
     */
    private void applyRanksSequentially(List<Player> players, int playerIndex, String lobbyId, Runnable completionCallback) {
        if (playerIndex >= players.size()) {
            completionCallback.run(); // Hoàn thành tất cả players trong lobby này
            return;
        }

        Player player = players.get(playerIndex);
        player.setRank(playerIndex + 1); // Gán rank (1, 2, 3...)

        playerService.updatePlayer(player.getUsername(), lobbyId, player, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Thành công: Chuyển sang player tiếp theo
                applyRanksSequentially(players, playerIndex + 1, lobbyId, completionCallback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi cập nhật rank cho player " + player.getUsername() + ". Tiếp tục player tiếp theo.", e);
                // Thất bại: Vẫn chuyển sang player tiếp theo
                applyRanksSequentially(players, playerIndex + 1, lobbyId, completionCallback);
            }
        });
    }

    /**
     * BƯỚC CUỐI: Tải Thống Kê (Được gọi sau khi Rank đã được cập nhật)
     */
    private void loadUserRankStatistics() {
        // NOTE: View visibility set to GONE here is now safe because the caller is wrapped in runOnUiThread
        if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);

        playerService.getRecentPlayerHistory(username, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String rawJson) {
                runOnUiThread(() -> {
                    JsonArray jsonArray;
                    try {
                        jsonArray = JsonParser.parseString(rawJson).getAsJsonArray();
                    } catch (JsonSyntaxException e) {
                        Log.e(TAG, "Lỗi phân tích JSON từ PlayerService: ", e);
                        if (textViewNoHistory != null) {
                            textViewNoHistory.setText("Lỗi phân tích dữ liệu: Không phải JSON hợp lệ.");
                            textViewNoHistory.setVisibility(View.VISIBLE);
                        }
                        return;
                    }


                    if (jsonArray.size() == 0) {
                        if (textViewNoHistory != null) {
                            textViewNoHistory.setText(getString(R.string.no_history_available));
                            textViewNoHistory.setVisibility(View.VISIBLE);
                        }
                    } else {

                        // === TẠO THỐNG KÊ RANK COUNT ===
                        Map<Integer, Integer> rankCounts = new HashMap<>();
                        int totalGames = 0;

                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject playerJson = jsonArray.get(i).getAsJsonObject();

                            if (playerJson.has("rank") && !playerJson.get("rank").isJsonNull()) {
                                int rank = playerJson.get("rank").getAsInt();
                                if (rank > 0) {
                                    rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
                                    totalGames++;
                                }
                            }
                        }

                        // === TẠO CHUỖI HIỂN THỊ THỐNG KÊ ===
                        List<String> statsItems = new ArrayList<>();

                        if (totalGames == 0) {
                            if (textViewNoHistory != null) {
                                textViewNoHistory.setText("Không có ván chơi nào được xếp hạng chính thức.");
                                textViewNoHistory.setVisibility(View.VISIBLE);
                            }
                            return;
                        }

                        // Sắp xếp Map theo thứ hạng (key: 1, 2, 3...)
                        List<Integer> sortedRanks = rankCounts.keySet().stream()
                                .sorted()
                                .collect(Collectors.toList());

                        statsItems.add(String.format("Tổng số ván đã hoàn thành: %d", totalGames));
                        statsItems.add("------------------------------------");

                        for (Integer rank : sortedRanks) {
                            int count = rankCounts.get(rank);
                            if (rank == 1) {
                                statsItems.add(String.format(Locale.getDefault(), "🏆 Hạng %d: %d lần", rank, count));
                            } else {
                                statsItems.add(String.format(Locale.getDefault(), "Hạng %d: %d lần", rank, count));
                            }
                        }
                        // ====================================================

                        adapter.clear();
                        adapter.addAll(statsItems);
                        adapter.notifyDataSetChanged();
                        if (lvHistory != null) lvHistory.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    if (progressBarLoading != null) progressBarLoading.setVisibility(View.GONE);
                    if (textViewNoHistory != null) {
                        textViewNoHistory.setText(getString(R.string.error_loading_history) + ": Lỗi: " + e.getMessage());
                        textViewNoHistory.setVisibility(View.VISIBLE);
                    }
                    Log.e(TAG, "Lỗi tải lịch sử xếp hạng", e);
                    Toast.makeText(LeaderHistoryActivity.this, "Lỗi tải dữ liệu.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}