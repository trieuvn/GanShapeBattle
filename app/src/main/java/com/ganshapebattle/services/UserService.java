package com.ganshapebattle.services;

import com.ganshapebattle.models.User;
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

public class UserService {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String supabaseUrl = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";
    private final String TABLE_NAME = "User";

    public void getAllUsers(SupabaseCallback<List<User>> callback) {
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
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(responseData, listType);
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void getUserByUsername(String username, SupabaseCallback<User> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?username=eq." + username + "&select=*")
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
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(responseData, listType);
                    if (users != null && !users.isEmpty()) {
                        callback.onSuccess(users.get(0));
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void insertUser(User user, SupabaseCallback<String> callback) {
        String jsonBody = gson.toJson(user);
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
                    callback.onSuccess("Thêm User thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void updateUser(String username, User user, SupabaseCallback<String> callback) {
        String jsonBody = gson.toJson(user);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?username=eq." + username)
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
                    callback.onSuccess("Cập nhật User thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void deleteUser(String username, SupabaseCallback<String> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?username=eq." + username)
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
                    callback.onSuccess("Xóa User thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }
}