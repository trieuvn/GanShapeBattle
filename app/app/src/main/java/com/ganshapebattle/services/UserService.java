package com.ganshapebattle.services;

import android.util.Log;

import com.ganshapebattle.models.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final String BUCKET_NAME = "avatars";

    // ================= PHẦN MỚI BẮT ĐẦU =================

    /**
     * Xác thực người dùng bằng email và mật khẩu qua API Auth của Supabase.
     * @param email Email của người dùng.
     * @param password Mật khẩu của người dùng.
     * @param callback Callback để xử lý kết quả (thành công hoặc thất bại).
     */
    public void loginUser(String email, String password, SupabaseCallback<String> callback) {
        // URL cho việc xác thực qua email/password của Supabase
        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

        // Tạo body cho request dưới dạng JSON
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);
        String jsonBody = gson.toJson(credentials);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // Xây dựng request
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
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
                    // Bạn có thể parse response.body().string() để lấy access_token nếu cần
                    callback.onSuccess("Đăng nhập thành công!");
                } else {
                    callback.onFailure(new IOException("Email hoặc mật khẩu không đúng. Mã lỗi: " + response.code()));
                }
            }
        });
    }
    // ================== PHẦN MỚI BẮT ĐẦU ==================

    /**
     * Đăng ký người dùng mới qua API Auth của Supabase.
     */
    public void registerUser(String email, String password, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/signup";

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);
        String jsonBody = gson.toJson(credentials);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
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
                    callback.onSuccess("Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.");
                } else {
                    String errorBody = response.body().string();
                    // Parse lỗi từ Supabase để thông báo rõ hơn, ví dụ "User already registered"
                    callback.onFailure(new IOException("Đăng ký thất bại: " + errorBody));
                }
            }
        });
    }

    /**
     * Gửi yêu cầu khôi phục mật khẩu qua API Auth của Supabase.
     */
    public void sendPasswordReset(String email, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/recover";

        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", email);
        String jsonBody = gson.toJson(emailMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
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
                    callback.onSuccess("Nếu email tồn tại, một liên kết khôi phục đã được gửi.");
                } else {
                    callback.onFailure(new IOException("Yêu cầu thất bại. Mã lỗi: " + response.code()));
                }
            }
        });
    }

    // =================== PHẦN MỚI KẾT THÚC ===================


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
    /**
     * Tải ảnh lên Supabase Storage và trả về URL công khai của ảnh.
     * @param fileName Tên file để lưu trên storage (ví dụ: "user1_avatar.jpg").
     * @param imageData Dữ liệu ảnh dưới dạng mảng byte.
     * @param callback Callback để xử lý kết quả.
     */
    public void uploadAvatar(String fileName, byte[] imageData, SupabaseCallback<String> callback) {
        // Tạo URL để upload lên Storage
        String url = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;

        // Tạo body của request từ dữ liệu ảnh
        RequestBody body = RequestBody.create(imageData, MediaType.get("image/jpeg"));

        // Xây dựng request
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true") // Ghi đè nếu file đã tồn tại
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
                    // Sau khi upload thành công, xây dựng URL công khai để lưu vào database
                    String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                    callback.onSuccess(publicUrl);
                } else {
                    callback.onFailure(new IOException("Lỗi upload ảnh: " + response.code() + " " + response.body().string()));
                }
            }
        });
    }
    // Thêm phương thức này vào bên trong class UserService

    public void updateUserPassword(String accessToken, String newPassword, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/user";

        Map<String, String> passwordMap = new HashMap<>();
        passwordMap.put("password", newPassword);
        String jsonBody = gson.toJson(passwordMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + accessToken) // Dùng token để xác thực
                .put(body) // Sử dụng phương thức PUT
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Cập nhật mật khẩu thành công!");
                } else {
                    callback.onFailure(new IOException("Không thể cập nhật mật khẩu. Lỗi: " + response.code()));
                }
            }
        });
    }
    // Thêm 2 phương thức này vào bên trong class UserService

    /**
     * Gửi yêu cầu gửi mã OTP reset mật khẩu đến email người dùng.
     */
    public void sendPasswordResetOtp(String email, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/otp";

        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", email);
        // Bạn có thể thêm create_user: false để đảm bảo không tạo user mới nếu email không tồn tại
        // emailMap.put("create_user", "false");
        String jsonBody = gson.toJson(emailMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
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
                    callback.onSuccess("Mã OTP đã được gửi đến email của bạn.");
                } else {
                    callback.onFailure(new IOException("Không thể gửi OTP. Lỗi: " + response.body().string()));
                }
            }
        });
    }

    /**
     * Xác thực mã OTP và trả về access_token để có thể đổi mật khẩu.
     */
    // Trong tệp UserService.java

    // Thay thế toàn bộ hàm cũ bằng hàm này trong UserService.java

    public void verifyPasswordResetOtp(String email, String otp, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/verify";

        Log.d("SupabaseDebug", "--- Bắt đầu xác thực OTP ---");
        Log.d("SupabaseDebug", "URL được gọi: " + url);
        Log.d("SupabaseDebug", "Email gửi đi: " + email);
        Log.d("SupabaseDebug", "OTP (Token) gửi đi: " + otp);

        Map<String, String> verificationData = new HashMap<>();
        verificationData.put("email", email);
        verificationData.put("token", otp);
        verificationData.put("type", "recovery");
        String jsonBody = gson.toJson(verificationData);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SupabaseDebug", "Yêu cầu thất bại onFailure.", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string(); // Đọc body ra trước
                if (response.isSuccessful()) {
                    Log.d("SupabaseDebug", "Xác thực OTP thành công! Body: " + responseBody);
                    try {
                        // SỬA LỖI Ở ĐÂY: Dùng Map<String, Object> để xử lý các kiểu dữ liệu khác nhau
                        Type type = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> responseMap = gson.fromJson(responseBody, type);

                        String accessToken = (String) responseMap.get("access_token");

                        if (accessToken != null) {
                            callback.onSuccess(accessToken);
                        } else {
                            callback.onFailure(new IOException("Không nhận được access token từ response."));
                        }
                    } catch (Exception e) {
                        Log.e("SupabaseDebug", "Lỗi khi parse JSON thành công.", e);
                        callback.onFailure(e);
                    }
                } else {
                    Log.e("SupabaseDebug", "Xác thực OTP thất bại. Mã lỗi: " + response.code() + ", Body: " + responseBody);
                    callback.onFailure(new IOException("Mã OTP không hợp lệ. Lỗi: " + responseBody));
                }
            }
        });
    }

// Hàm updateUserPassword đã có sẵn của bạn sẽ được tái sử dụng, không cần thay đổi.
    // Thêm phương thức này vào bên trong class UserService

    /**
     * Xác thực mã OTP cho việc đăng ký tài khoản mới.
     */
    public void verifySignupOtp(String email, String otp, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/verify";

        Map<String, String> verificationData = new HashMap<>();
        verificationData.put("email", email);
        verificationData.put("token", otp);
        verificationData.put("type", "signup"); // <-- Type là "signup"
        String jsonBody = gson.toJson(verificationData);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
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
                    // Xác thực thành công, tài khoản đã được kích hoạt
                    callback.onSuccess("Xác thực tài khoản thành công!");
                } else {
                    callback.onFailure(new IOException("Mã OTP không hợp lệ. Lỗi: " + response.body().string()));
                }
            }
        });
    }
}