package com.ganshapebattle.services;

import android.util.Log;

import androidx.annotation.NonNull; // <<< Import NonNull

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
    // Thay thế bằng URL và Key của bạn nếu khác
    private final String supabaseUrl = "https://cggimbfrkwjexvtaabbq.supabase.co";
    private final String supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNnZ2ltYmZya3dqZXh2dGFhYmJxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA1NDk1NDUsImV4cCI6MjA3NjEyNTU0NX0.78h5Lzrr_APZvi99MESsRDukcprXhG8pbX9UVqKuOcA";
    private final String TABLE_NAME = "User"; // Tên bảng public User của bạn
    private final String BUCKET_NAME = "avatars"; // Tên bucket lưu avatar nếu có
    private static final String TAG = "UserService"; // Tag để lọc log

    /**
     * Xác thực người dùng bằng email và mật khẩu qua API Auth của Supabase.
     */
    public void loginUser(String email, String password, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/token?grant_type=password";

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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Đăng nhập thành công!");
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Email hoặc mật khẩu không đúng. Mã lỗi: " + response.code() + " " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close(); // Đảm bảo đóng body
                    }
                }
            }
        });
    }

    /**
     * Đăng ký người dùng mới qua API Auth của Supabase và đồng bộ sang bảng public.User.
     * Vẫn gọi checkAndSyncUser vì mật khẩu có sẵn ngay lúc đăng ký.
     */
    public void registerUser(String email, String password, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/signup";

        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password); // Mật khẩu được gửi đến Supabase Auth
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = null;
                try {
                    assert response.body() != null;
                    responseBody = response.body().string(); // Đọc body
                    if (response.isSuccessful()) {
                        try {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> userMap = gson.fromJson(responseBody, type);
                            String authId = (String) userMap.get("id");
                            String userEmail = (String) userMap.get("email");
                            if (authId != null && userEmail != null) {
                                Log.d(TAG, "Đăng ký thành công, chuẩn bị gọi checkAndSyncUser cho: " + userEmail);
                                // Gọi checkAndSyncUser với mật khẩu đã có
                                checkAndSyncUser(authId, userEmail, null, null, password);
                            } else {
                                Log.e(TAG, "Đăng ký thành công nhưng không lấy được authId hoặc email từ response.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi khi parse response đăng ký: " + e.getMessage());
                        }
                        callback.onSuccess("Đăng ký thành công! Vui lòng kiểm tra email để xác nhận.");
                    } else {
                        callback.onFailure(new IOException("Đăng ký thất bại: " + responseBody));
                    }
                } finally {
                    if (response.body() != null && responseBody == null) { // Đảm bảo body được đóng nếu chưa đọc
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Gửi yêu cầu khôi phục mật khẩu (gửi link qua email) qua API Auth của Supabase.
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Nếu email tồn tại, một liên kết khôi phục đã được gửi.");
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Yêu cầu thất bại. Mã lỗi: " + response.code() + " " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Gửi yêu cầu gửi mã OTP reset mật khẩu đến email người dùng.
     */
    public void sendPasswordResetOtp(String email, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/otp";

        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", email);
        String jsonBody = gson.toJson(emailMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Mã OTP đã được gửi đến email của bạn.");
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Không thể gửi OTP. Lỗi: " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Xác thực mã OTP reset mật khẩu và trả về access_token.
     */
    public void verifyPasswordResetOtp(String email, String otp, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/verify";

        Log.d("SupabaseDebug", "--- Bắt đầu xác thực OTP Reset PW ---");
        Log.d("SupabaseDebug", "Email: " + email + ", OTP: " + otp);

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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SupabaseDebug", "Xác thực OTP Reset PW thất bại onFailure.", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = null;
                try {
                    assert response.body() != null;
                    responseBody = response.body().string(); // Đọc body ra trước
                    if (response.isSuccessful()) {
                        Log.d("SupabaseDebug", "Xác thực OTP Reset PW thành công! Body: " + responseBody);
                        try {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> responseMap = gson.fromJson(responseBody, type);
                            String accessToken = (String) responseMap.get("access_token");
                            if (accessToken != null) {
                                callback.onSuccess(accessToken);
                            } else {
                                callback.onFailure(new IOException("Không nhận được access token từ response OTP Reset PW."));
                            }
                        } catch (Exception e) {
                            Log.e("SupabaseDebug", "Lỗi parse JSON OTP Reset PW thành công.", e);
                            callback.onFailure(e);
                        }
                    } else {
                        Log.e("SupabaseDebug", "Xác thực OTP Reset PW thất bại. Code: " + response.code() + ", Body: " + responseBody);
                        callback.onFailure(new IOException("Mã OTP Reset PW không hợp lệ. Lỗi: " + responseBody));
                    }
                } finally {
                    if (response.body() != null && responseBody == null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Cập nhật mật khẩu người dùng trong Supabase Auth bằng access token.
     */
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
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Cập nhật mật khẩu Auth thành công!");
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Không thể cập nhật mật khẩu Auth. Lỗi: " + response.code() + " " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Xác thực mã OTP cho việc đăng ký tài khoản mới.
     */
    public void verifySignupOtp(String email, String otp, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/auth/v1/verify";

        Map<String, String> verificationData = new HashMap<>();
        verificationData.put("email", email);
        verificationData.put("token", otp);
        verificationData.put("type", "signup");
        String jsonBody = gson.toJson(verificationData);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Xác thực tài khoản thành công!");
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Mã OTP không hợp lệ. Lỗi: " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * Đăng nhập bằng Google ID Token và trả về session.
     * KHÔNG còn gọi checkAndSyncUser nữa, việc này được xử lý sau khi tạo mật khẩu.
     */
    public void signInWithGoogle(String idToken, SupabaseCallback<Map<String, Object>> callback) {
        String url = supabaseUrl + "/auth/v1/token?grant_type=id_token";

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("id_token", idToken);
        bodyMap.put("provider", "google");

        String jsonBody = gson.toJson(bodyMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = null;
                try {
                    assert response.body() != null;
                    responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        try {
                            Type type = new TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> sessionMap = gson.fromJson(responseBody, type);
                            Log.d(TAG, "signInWithGoogle thành công.");
                            // Không gọi checkAndSyncUser ở đây nữa
                            callback.onSuccess(sessionMap);
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi khi parse response Google sign-in: " + e.getMessage());
                            callback.onFailure(e);
                        }
                    } else {
                        Log.e(TAG, "signInWithGoogle thất bại: " + responseBody);
                        callback.onFailure(new IOException("Đăng nhập Google Supabase thất bại: " + responseBody));
                    }
                } finally {
                    if (response.body() != null && responseBody == null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    // =================== CRUD Operations cho bảng public.User ===================

    /**
     * Lấy tất cả user từ bảng public.User.
     */
    public void getAllUsers(SupabaseCallback<List<User>> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?select=*")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền đọc
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = null;
                String errorBody = null;
                try {
                    assert response.body() != null;
                    responseData = response.body().string();
                    if (response.isSuccessful()) {
                        Type listType = new TypeToken<List<User>>() {}.getType();
                        List<User> users = gson.fromJson(responseData, listType);
                        callback.onSuccess(users);
                    } else {
                        errorBody = responseData; // Đã đọc body rồi
                        Log.e(TAG, "Lỗi getAllUsers: Code=" + response.code() + ", Body=" + errorBody);
                        callback.onFailure(new IOException("Lỗi getAllUsers: " + response.code() + " " + errorBody));
                    }
                } finally {
                    // Body đã được đọc hoặc không có body để đóng
                }
            }
        });
    }

    /**
     * Lấy một user từ bảng public.User dựa trên email.
     */
    public void getUserByEmail(String email, SupabaseCallback<User> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?email=eq." + email + "&select=*&limit=1")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền đọc
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = null;
                try {
                    assert response.body() != null;
                    responseData = response.body().string();
                    if (response.isSuccessful()) {
                        Type listType = new TypeToken<List<User>>() {}.getType();
                        List<User> users = gson.fromJson(responseData, listType);
                        callback.onSuccess(users != null && !users.isEmpty() ? users.get(0) : null);
                    } else {
                        callback.onFailure(new IOException("Lỗi getUserByEmail: " + response.code() + " " + responseData));
                    }
                } finally {
                    // Body đã được đọc
                }
            }
        });
    }

    /**
     * Lấy một user từ bảng public.User dựa trên username.
     */
    public void getUserByUsername(String username, SupabaseCallback<User> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?username=eq." + username + "&select=*&limit=1")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền đọc
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = null;
                try {
                    assert response.body() != null;
                    responseData = response.body().string();
                    if (response.isSuccessful()) {
                        Type listType = new TypeToken<List<User>>() {}.getType();
                        List<User> users = gson.fromJson(responseData, listType);
                        callback.onSuccess(users != null && !users.isEmpty() ? users.get(0) : null);
                    } else {
                        callback.onFailure(new IOException("Lỗi getUserByUsername: " + response.code() + " " + responseData));
                    }
                } finally {
                    // Body đã được đọc
                }
            }
        });
    }

    /**
     * Thêm một user mới vào bảng public.User.
     * Được gọi từ checkAndSyncUser (khi đăng ký email) hoặc từ CreatePasswordActivity.
     */
    public void insertUser(User user, SupabaseCallback<String> callback) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            if (user.getEmail() != null && user.getEmail().contains("@")) {
                user.setUsername(user.getEmail().split("@")[0]);
                Log.w(TAG, "insertUser: Username bị trống, tự động tạo từ email: " + user.getUsername());
            } else {
                callback.onFailure(new IllegalArgumentException("Username không được để trống khi insert và không thể tạo từ email"));
                return;
            }
        }
        // Mật khẩu được đặt bởi nơi gọi (CreatePasswordActivity hoặc checkAndSyncUser)

        String jsonBody = gson.toJson(user);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền insert
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String errorBody = null;
                try {
                    assert response.body() != null;
                    errorBody = response.body().string(); // Đọc body luôn để log lỗi
                    if (response.isSuccessful()) {
                        callback.onSuccess("Thêm User thành công");
                    } else {
                        Log.e(TAG, "Lỗi insertUser: Code=" + response.code() + ", Body=" + errorBody);
                        callback.onFailure(new IOException("Lỗi insertUser: " + response.code() + " " + errorBody));
                    }
                } finally {
                    // Body đã được đọc
                }
            }
        });
    }

    /**
     * Cập nhật thông tin user trong bảng public.User dựa trên email.
     * Chỉ cập nhật các trường được cung cấp trong `user` object.
     * SỬA LỖI: Thêm username vào updateMap.
     */
    public void updateUser(String email, User user, SupabaseCallback<String> callback) {
        Map<String, Object> updateMap = new HashMap<>();

        // === THÊM DÒNG NÀY ===
        if (user.getUsername() != null) updateMap.put("username", user.getUsername());
        // ======================

        if (user.getPassword() != null) updateMap.put("password", user.getPassword());
        if (user.getPhoneNumber() != null) updateMap.put("phoneNumber", user.getPhoneNumber());
        if (user.getDateOfBirth() != null) updateMap.put("dateOfBirth", user.getDateOfBirth());
        if (user.getAvatar() != null) updateMap.put("avatar", user.getAvatar());

        if (updateMap.isEmpty()) {
            Log.w(TAG, "updateUser: Không có trường nào để cập nhật cho email: " + email);
            callback.onSuccess("Không có gì để cập nhật.");
            return;
        }

        String jsonBody = gson.toJson(updateMap);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?email=eq." + email) // Dùng email để lọc
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền update
                .addHeader("Content-Type", "application/json")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String bodyString = null;
                try {
                    assert response.body() != null;
                    bodyString = response.body().string(); // Đọc body một lần
                    if (response.isSuccessful()) {
                        callback.onSuccess("Cập nhật User thành công");
                    } else {
                        if (response.code() == 406 || (bodyString != null && bodyString.equals("[]"))) {
                            callback.onFailure(new IOException("Không tìm thấy user với email: " + email + " để cập nhật."));
                        } else {
                            Log.e(TAG, "Lỗi updateUser: Code=" + response.code() + ", Body=" + bodyString);
                            callback.onFailure(new IOException("Lỗi updateUser: " + response.code() + " " + bodyString));
                        }
                    }
                } finally {
                    // Body đã được đọc hoặc không có body để đóng
                }
            }
        });
    }

    /**
     * Xóa user khỏi bảng public.User dựa trên email.
     */
    public void deleteUser(String email, SupabaseCallback<String> callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/" + TABLE_NAME + "?email=eq." + email) // Dùng email để lọc
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền delete
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String bodyString = null;
                try {
                    assert response.body() != null;
                    bodyString = response.body().string(); // Đọc body
                    if (response.isSuccessful()) { // DELETE thành công thường là 204 No Content
                        callback.onSuccess("Xóa User (nếu tồn tại) thành công");
                    } else {
                        if (response.code() == 406) { // Lỗi nếu dùng return=rep mà không tìm thấy
                            Log.w(TAG, "Không tìm thấy user với email: " + email + " để xóa (Code 406).");
                            callback.onSuccess("Không tìm thấy user để xóa."); // Coi như thành công
                        } else {
                            Log.e(TAG, "Lỗi deleteUser: Code=" + response.code() + ", Body=" + bodyString);
                            callback.onFailure(new IOException("Lỗi deleteUser: " + response.code() + " " + bodyString));
                        }
                    }
                } finally {
                    // Body đã được đọc
                }
            }
        });
    }

    /**
     * Tải ảnh avatar lên Supabase Storage.
     */
    public void uploadAvatar(String fileName, byte[] imageData, SupabaseCallback<String> callback) {
        String url = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;
        RequestBody body = RequestBody.create(imageData, MediaType.get("image/jpeg"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey) // Cần token có quyền upload
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + fileName;
                        callback.onSuccess(publicUrl);
                    } else {
                        assert response.body() != null;
                        callback.onFailure(new IOException("Lỗi upload ảnh: " + response.code() + " " + response.body().string()));
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    // =================== Logic Đồng bộ (Chỉ dùng cho đăng ký Email) ===================

    /**
     * Hàm nội bộ kiểm tra và đồng bộ user từ auth.users sang public.User khi đăng ký bằng email.
     * Lưu mật khẩu plain text nếu được cung cấp.
     */
    private void checkAndSyncUser(String authId, String email, String avatarUrl, String fullName, String password) {
        if (email == null) {
            Log.e(TAG, "checkAndSyncUser (Email Reg): Email bị null.");
            return;
        }

        Log.d(TAG, "checkAndSyncUser (Email Reg): Bắt đầu kiểm tra email: " + email);

        getUserByEmail(email, new SupabaseCallback<User>() {
            @Override
            public void onSuccess(User existingUser) {
                Log.d(TAG, "checkAndSyncUser (Email Reg): Kết quả getUserByEmail: " + (existingUser == null ? "Không tìm thấy" : "Đã tìm thấy"));

                if (existingUser == null) {
                    User newUser = new User();
                    String generatedUsername = email.split("@")[0];
                    // TODO: Xử lý username trùng lặp nếu cần
                    newUser.setUsername(generatedUsername);
                    newUser.setEmail(email);
                    newUser.setAvatar(avatarUrl); // Sẽ là null khi đăng ký email
                    newUser.setPassword(password); // Lưu password từ form đăng ký
                    newUser.setPhoneNumber(null);
                    newUser.setDateOfBirth(null);

                    Log.d(TAG, "checkAndSyncUser (Email Reg): Gọi insertUser cho: " + email);
                    insertUser(newUser, new SupabaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            Log.d(TAG, "Đồng bộ user mới (Email Reg) thành công: " + email);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "Lỗi khi đồng bộ (insertUser) user mới (Email Reg): " + email, e);
                        }
                    });
                } else {
                    Log.d(TAG, "checkAndSyncUser (Email Reg): User " + email + " đã tồn tại. Kiểm tra cập nhật password...");
                    // Chỉ cập nhật password nếu nó khác (trường hợp hiếm khi đăng ký lại email đã tồn tại)
                    if (password != null && !password.equals(existingUser.getPassword())) {
                        User userToUpdate = new User();
                        userToUpdate.setPassword(password);
                        updateUser(email, userToUpdate, new SupabaseCallback<String>() {
                            @Override public void onSuccess(String result) { Log.d(TAG, "Cập nhật password cho user đã tồn tại (Email Reg) thành công: " + email); }
                            @Override public void onFailure(Exception e) { Log.e(TAG, "Lỗi cập nhật password cho user đã tồn tại (Email Reg): " + email, e); }
                        });
                    } else {
                        Log.d(TAG, "checkAndSyncUser (Email Reg): Không cần cập nhật password.");
                    }
                }
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi nghiêm trọng khi kiểm tra user bằng email (Email Reg): " + email, e);
            }
        });
    }

} // Kết thúc class UserService