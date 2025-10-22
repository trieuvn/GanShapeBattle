package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("beginDate")
    private String beginDate;

    // Constructors
    public Lobby() {
    }

    public Lobby(String id, String adminUsername, String mode, String status, int maxPlayer, int designTime, int voteTime, String createdDate, String beginDate) {
        this.id = id;
        this.adminUsername = adminUsername;
        this.mode = mode;
        this.status = status;
        this.maxPlayer = maxPlayer;
        this.designTime = designTime;
        this.voteTime = voteTime;
        this.createdDate = createdDate;
        this.beginDate = beginDate;
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

    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public String getBeginDate() { return beginDate; }
    public void setBeginDate(String beginDate) { this.beginDate = beginDate; }

    // --- Additional Methods ---

    /**
     * Lấy trạng thái tiếp theo của lobby
     * Logic: null -> isPlaying, idle -> isPlaying, isPlaying -> isVoting -> isOver -> isEnd
     * @return Trạng thái tiếp theo của lobby
     */
    public String getNextStatus() {
        if (status == null) {
            return "isPlaying";
        }
        
        switch (status) {
            case "idle":
                return "isPlaying";
            case "isPlaying":
                return "isVoting";
            case "isVoting":
                return "isOver";
            case "isOver":
                return "isEnd";
            default:
                return "isPlaying";
        }
    }

    /**
     * Lấy danh sách players trong lobby này
     * @param callback Callback để xử lý kết quả
     */
    public void getPlayers(SupabaseCallback<List<Player>> callback) {
        PlayerService playerService = new PlayerService();
        // Lấy tất cả players có lobbyId = this.id
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                // Lọc ra chỉ những players của lobby này
                List<Player> lobbyPlayers = allPlayers.stream()
                    .filter(player -> id.equals(player.getLobbyId()))
                    .collect(java.util.stream.Collectors.toList());
                callback.onSuccess(lobbyPlayers);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Lấy thông tin admin của lobby này
     * @param callback Callback để xử lý kết quả
     */
    public void getAdmin(SupabaseCallback<User> callback) {
        UserService userService = new UserService();
        // Lấy user có username = this.adminUsername
        userService.getUserByUsername(adminUsername, callback);
    }

    /**
     * Tính toán ngày bắt đầu vote = beginDate + designTime (phút)
     * @return Chuỗi ngày tháng bắt đầu vote
     */
    public String getBeginVoteDate() {
        if (beginDate == null || beginDate.isEmpty()) {
            return null;
        }
        
        try {
            // Parse beginDate thành LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime beginDateTime = LocalDateTime.parse(beginDate, formatter);
            
            // Thêm designTime (phút)
            LocalDateTime beginVoteDateTime = beginDateTime.plusMinutes(designTime);
            
            // Format lại thành chuỗi
            return beginVoteDateTime.format(formatter);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tính toán ngày kết thúc = beginDate + designTime + (voteTime * số lượng players) (phút)
     * @param callback Callback để xử lý kết quả (cần số lượng players)
     */
    public void getEndDate(SupabaseCallback<String> callback) {
        if (beginDate == null || beginDate.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        
        // Lấy danh sách players để tính số lượng
        getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                try {
                    // Parse beginDate thành LocalDateTime
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime beginDateTime = LocalDateTime.parse(beginDate, formatter);
                    
                    // Tính tổng thời gian: designTime + (voteTime * số players)
                    int totalMinutes = designTime + (voteTime * players.size());
                    
                    // Thêm tổng thời gian vào beginDate
                    LocalDateTime endDateTime = beginDateTime.plusMinutes(totalMinutes);
                    
                    // Format lại thành chuỗi
                    String endDate = endDateTime.format(formatter);
                    callback.onSuccess(endDate);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}