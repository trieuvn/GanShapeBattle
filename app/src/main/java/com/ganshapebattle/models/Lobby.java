package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;

public class Lobby {

    @SerializedName("id")
    private String id;

    @SerializedName("admin")
    private String adminUsername;

    @SerializedName("mode")
    private String mode;

    @SerializedName("status")
    private String status;

    @SerializedName("maxPlayer")
    private int maxPlayer;

    @SerializedName("designTime")
    private int designTime;

    @SerializedName("voteTime")
    private int voteTime;

    // Constructors
    public Lobby() {
    }

    public Lobby(String id, String adminUsername, String mode, String status, int maxPlayer, int designTime, int voteTime) {
        this.id = id;
        this.adminUsername = adminUsername;
        this.mode = mode;
        this.status = status;
        this.maxPlayer = maxPlayer;
        this.designTime = designTime;
        this.voteTime = voteTime;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMaxPlayer() { return maxPlayer; }
    public void setMaxPlayer(int maxPlayer) { this.maxPlayer = maxPlayer; }
    public int getDesignTime() { return designTime; }
    public void setDesignTime(int designTime) { this.designTime = designTime; }
    public int getVoteTime() { return voteTime; }
    public void setVoteTime(int voteTime) { this.voteTime = voteTime; }
}