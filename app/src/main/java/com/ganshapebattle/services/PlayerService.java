package com.ganshapebattle.services;

import com.ganshapebattle.models.Player;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PlayerService {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String supabaseUrl = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";
    private final String TABLE_NAME = "Player";

    /**
     * Lấy lịch sử xếp hạng gần đây của một người dùng.
     * Sửa lỗi: Thay đổi callback thành String để trả về JSON thô.
     */
    public void getRecentPlayerHistory(String username, SupabaseCallback<String> callback) { // <-- CHỮ KÝ ĐÃ ĐỔI
        String url = supabaseUrl + "/rest/v1/" + TABLE_NAME
                + "?user=eq." + username
                + "&select=*,lobby(mode,createdDate)"
                + "&limit=20";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    callback.onSuccess(responseData); // <-- TRẢ VỀ JSON STRING THÔ
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void getAllPlayers(SupabaseCallback<List<Player>> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Type listType = new TypeToken<List<Player>>() {}.getType();
                    List<Player> players = gson.fromJson(responseData, listType);
                    callback.onSuccess(players);
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void getPlayerByIds(String username, String lobbyId, SupabaseCallback<Player> callback) {
        String url = supabaseUrl + "/rest/v1/" + TABLE_NAME + "?user=eq." + username + "&lobby=eq." + lobbyId + "&select=*";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Type listType = new TypeToken<List<Player>>() {}.getType();
                    List<Player> players = gson.fromJson(responseData, listType);
                    if (players != null && !players.isEmpty()) {
                        callback.onSuccess(players.get(0));
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void insertPlayer(Player player, SupabaseCallback<String> callback) {
        String jsonBody = gson.toJson(player);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Thêm Player thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void updatePlayer(String username, String lobbyId, Player player, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/rest/v1/" + TABLE_NAME + "?user=eq." + username + "&lobby=eq." + lobbyId;
        String jsonBody = gson.toJson(player);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Cập nhật Player thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void deletePlayer(String username, String lobbyId, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/rest/v1/" + TABLE_NAME + "?user=eq." + username + "&lobby=eq." + lobbyId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Xóa Player thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }
}