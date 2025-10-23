package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;

public class User {

    // @SerializedName dùng để khớp tên biến Java (camelCase) với tên cột
    // trong database (thường là snake_case hoặc lowercase).
    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("email")
    private String email;

    @SerializedName("phoneNumber")
    private String phoneNumber;

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("avatar")
    private String avatar;

    // --- Constructors (Phương thức khởi tạo) ---

    /**
     * Constructor trống. Cần thiết cho thư viện Gson để tạo đối tượng từ JSON.
     */
    public User() {
    }

    /**
     * Constructor đầy đủ để bạn có thể tự tạo đối tượng User trong code.
     */
    public User(String username, String password, String email, String phoneNumber, String dateOfBirth, String avatar) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.avatar = avatar;
    }


    // --- Getters (Để lấy giá trị) ---

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAvatar() {
        return avatar;
    }


    // --- Setters (Để thay đổi giá trị) ---

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}