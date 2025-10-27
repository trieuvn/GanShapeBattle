package com.ganshapebattle.services;

import com.ganshapebattle.models.Picture;
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

public class PictureService {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String supabaseUrl = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";
    private final String TABLE_NAME = "Picture";
    private final String BUCKET_NAME = "pictures"; // <-- THÊM DÒNG NÀY
    public void getAllPictures(SupabaseCallback<List<Picture>> callback) {
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
                    Type listType = new TypeToken<List<Picture>>() {}.getType();
                    List<Picture> pictures = gson.fromJson(responseData, listType);
                    callback.onSuccess(pictures);
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void getPictureById(String pictureId, SupabaseCallback<Picture> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?id=eq." + pictureId + "&select=*")
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
                    Type listType = new TypeToken<List<Picture>>() {}.getType();
                    List<Picture> pictures = gson.fromJson(responseData, listType);
                    if (pictures != null && !pictures.isEmpty()) {
                        callback.onSuccess(pictures.get(0));
                    } else {
                        callback.onSuccess(null);
                    }
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    //Tra ve Picture tu` supabase sau khi them
    public void insertPicture(Picture picture, SupabaseCallback<Picture> callback) {
        String jsonBody = gson.toJson(picture);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
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
                    String responseData = response.body().string();
                    Type listType = new TypeToken<List<Picture>>() {}.getType();
                    List<Picture> pictures = gson.fromJson(responseData, listType);
                    if (pictures != null && !pictures.isEmpty()) {
                        callback.onSuccess(pictures.get(0));
                    } else {
                        callback.onFailure(new IOException("Không thể lấy Picture sau khi thêm"));
                    }
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void updatePicture(String pictureId, Picture picture, SupabaseCallback<String> callback) {
        String jsonBody = gson.toJson(picture);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?id=eq." + pictureId)
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
                    callback.onSuccess("Cập nhật Picture thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }

    public void deletePicture(String pictureId, SupabaseCallback<String> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?id=eq." + pictureId)
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
                    callback.onSuccess("Xóa Picture thành công");
                } else {
                    callback.onFailure(new IOException("Lỗi: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }
    public void uploadPictureImage(String fileName, byte[] imageData, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;
        RequestBody body = RequestBody.create(imageData, MediaType.get("image/jpeg"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("x-upsert", "true")
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
                    String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                    callback.onSuccess(publicUrl);
                } else {
                    callback.onFailure(new IOException("Lỗi upload ảnh: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }
}