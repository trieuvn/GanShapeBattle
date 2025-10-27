package com.ganshapebattle.services;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class SupabaseService {

    private final OkHttpClient client = new OkHttpClient();

    // --- DÁN URL VÀ KEY MỚI CỦA BẠN VÀO ĐÂY ---
    private final String supabaseUrl = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";

    // Interface để làm callback, gửi kết quả về cho Activity
    public interface SupabaseCallback {
        void onSuccess(String result);
        void onFailure(Exception e);
    }

    // Phương thức để lấy dữ liệu từ bảng "User"
    public void getUsers(SupabaseCallback callback) {
        // Xây dựng URL để gọi API
        String url = supabaseUrl + "/rest/v1/User?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .build();

        // Thực thi yêu cầu mạng bất đồng bộ
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e); // Gửi lỗi về nếu thất bại
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    callback.onSuccess(responseData); // Gửi dữ liệu về nếu thành công
                } else {
                    // Gửi lỗi về kèm mã lỗi từ server (ví dụ: 404, 401)
                    callback.onFailure(new IOException("Yêu cầu thất bại với mã lỗi: " + response.code()));
                }
            }
        });
    }
}
