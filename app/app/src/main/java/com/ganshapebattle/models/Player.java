package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;

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
}