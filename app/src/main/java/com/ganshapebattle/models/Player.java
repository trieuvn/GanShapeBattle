package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;

public class Player {

    @SerializedName("user")
    private String username;

    @SerializedName("lobby")
    private String lobbyId;

    @SerializedName("point")
    private int point;

    @SerializedName("rank")
    private int rank;

    @SerializedName("picture")
    private String pictureId;

    // Constructors
    public Player() {
    }

    public Player(String username, String lobbyId, int point, int rank, String pictureId) {
        this.username = username;
        this.lobbyId = lobbyId;
        this.point = point;
        this.rank = rank;
        this.pictureId = pictureId;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getLobbyId() { return lobbyId; }
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    public int getPoint() { return point; }
    public void setPoint(int point) { this.point = point; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public String getPictureId() { return pictureId; }
    public void setPictureId(String pictureId) { this.pictureId = pictureId; }

    // --- Additional Methods ---

    /**
     * Lấy thông tin picture của player này
     * @param callback Callback để xử lý kết quả
     */
    public void getPicture(SupabaseCallback<Picture> callback) {
        if (pictureId == null || pictureId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        
        PictureService pictureService = new PictureService();
        // Lấy picture có id = this.pictureId
        pictureService.getPictureById(pictureId, callback);
    }

    /**
     * Lấy thông tin user của player này
     * @param callback Callback để xử lý kết quả
     */
    public void getUser(SupabaseCallback<User> callback) {
        if (username == null || username.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        
        UserService userService = new UserService();
        // Lấy user có username = this.username
        userService.getUserByUsername(username, callback);
    }

    /**
     * Lấy thông tin lobby của player này
     * @param callback Callback để xử lý kết quả
     */
    public void getLobby(SupabaseCallback<Lobby> callback) {
        if (lobbyId == null || lobbyId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        
        LobbyService lobbyService = new LobbyService();
        // Lấy lobby có id = this.lobbyId
        lobbyService.getLobbyById(lobbyId, callback);
    }
}