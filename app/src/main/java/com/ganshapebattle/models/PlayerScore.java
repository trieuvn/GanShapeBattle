package com.ganshapebattle.models;

// Lớp model đơn giản để chứa thông tin điểm số
public class PlayerScore {
    private String userId;
    private String username;
    private int score;

    // --- SỬA LỖI: Đổi từ avatarUrl sang pictureBase64 ---
    private String pictureBase64; // Sẽ chứa chuỗi Base64

    public PlayerScore() {
    }

    public PlayerScore(String userId, String username, int score, String pictureBase64) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.pictureBase64 = pictureBase64;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    // --- SỬA LỖI: Cập nhật Getters/Setters ---
    public String getPictureBase64() {
        return pictureBase64;
    }

    public void setPictureBase64(String pictureBase64) {
        this.pictureBase64 = pictureBase64;
    }
}