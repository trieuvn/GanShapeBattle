package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;

// Import API cũ, an toàn cho minSdk 24
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

// Xóa các import API java.time (LocalDateTime, DateTimeFormatter...)

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
    private int designTime; // Tính bằng GIÂY

    @SerializedName("voteTime")
    private int voteTime; // Tính bằng GIÂY

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("beginDate")
    private String beginDate;

    // --- Formatters (An toàn) ---
    // Định dạng 1: "yyyy-MM-dd HH:mm:ss" (Có dấu cách)
    private static final SimpleDateFormat simpleFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Định dạng 2: ISO 8601 "yyyy-MM-dd'T'HH:mm:ssZ" (dùng cho 'T' và '+0700')
    private static final SimpleDateFormat isoFormatterZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

    // Định dạng 3: ISO 8601 với 'Z' (UTC)
    private static final SimpleDateFormat isoFormatterUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    // --- THÊM ĐỊNH DẠNG 4 (GÂY LỖI) ---
    // Định dạng 4: "yyyy-MM-dd'T'HH:mm:ss" (Có 'T' nhưng KHÔNG có múi giờ)
    private static final SimpleDateFormat isoFormatterNoZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    static {
        // Đảm bảo "Z" luôn được hiểu là UTC
        isoFormatterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Đảm bảo format không có múi giờ cũng được hiểu là giờ địa phương (hoặc UTC)
        isoFormatterNoZone.setTimeZone(TimeZone.getTimeZone("UTC")); // Coi như là UTC nếu không rõ
    }


    // ... (Giữ nguyên Constructors, Getters, Setters) ...
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

    // ... (Giữ nguyên getNextStatus, getPlayers, getAdmin) ...
    public String getNextStatus() {
        if (status == null) {
            return "isPlaying";
        }
        switch (status) {
            case "idle": return "isPlaying";
            case "isPlaying": return "isVoting";
            case "isVoting": return "isOver";
            case "isOver": return "isEnd";
            default: return "isPlaying";
        }
    }
    public void getPlayers(SupabaseCallback<List<Player>> callback) {
        PlayerService playerService = new PlayerService();
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                // Lọc phía client (nếu cần)
                List<Player> lobbyPlayers = allPlayers.stream()
                        .filter(player -> id.equals(player.getLobbyId()))
                        .collect(Collectors.toList());
                callback.onSuccess(lobbyPlayers);
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    public void getAdmin(SupabaseCallback<User> callback) {
        UserService userService = new UserService();
        userService.getUserByUsername(adminUsername, callback);
    }


    // --- SỬA LỖI API (Dùng Calendar thay vì java.time) ---

    /**
     * Helper (mới) để parse (đọc) chuỗi thời gian (an toàn cho minSdk 24)
     * Hỗ trợ nhiều định dạng.
     */
    private Date parseDateString(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        // Thử định dạng 1: "yyyy-MM-dd HH:mm:ss" (Format bạn thấy trên Supabase)
        try {
            synchronized (simpleFormatter) {
                return simpleFormatter.parse(dateString);
            }
        } catch (ParseException e) {
            // Thử định dạng 4: "yyyy-MM-dd'T'HH:mm:ss" (Format 'T' không múi giờ)
            try {
                synchronized (isoFormatterNoZone) {
                    return isoFormatterNoZone.parse(dateString);
                }
            } catch (ParseException e2) {
                // Thử định dạng 2 & 3: ISO 8601 (có 'T' và có múi giờ 'Z' hoặc '+0700')
                try {
                    if (dateString.endsWith("Z")) {
                        synchronized (isoFormatterUTC) {
                            return isoFormatterUTC.parse(dateString);
                        }
                    }
                    synchronized (isoFormatterZone) {
                        return isoFormatterZone.parse(dateString);
                    }
                } catch (ParseException e3) {
                    e3.printStackTrace(); // Lỗi: Không thể parse BẤT KỲ định dạng nào
                    return null;
                }
            }
        }
    }

    /**
     * Tính toán ngày bắt đầu vote = beginDate + designTime (GIÂY)
     * (ĐÃ SỬA LỖI API)
     * @return Chuỗi ngày tháng bắt đầu vote (theo format "yyyy-MM-dd HH:mm:ss")
     */
    public String getBeginVoteDate() {
        // 1. Sử dụng hàm helper mới để parse
        Date beginDateTime = parseDateString(beginDate);

        if (beginDateTime == null) {
            // Đây là lý do bạn nhận lỗi "Không thể lấy thời gian"
            return null;
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(beginDateTime);

            // 2. Thêm designTime (SỐ GIÂY)
            calendar.add(Calendar.SECOND, designTime); // <-- Tính bằng GIÂY

            // 3. Format lại thành chuỗi (luôn là format "simple")
            synchronized (simpleFormatter) {
                return simpleFormatter.format(calendar.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Tính toán ngày kết thúc = beginDate + designTime + (voteTime * số lượng players) (GIÂY)
     * (CŨNG ĐÃ SỬA LỖI API)
     * @param callback Callback để xử lý kết quả
     */
    public void getEndDate(SupabaseCallback<String> callback) {
        // 1. Sử dụng hàm helper mới để parse
        Date beginDateTime = parseDateString(beginDate);

        if (beginDateTime == null) {
            callback.onSuccess(null);
            return;
        }

        getPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> players) {
                try {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(beginDateTime);

                    // 2. Tính tổng thời gian (BẰNG GIÂY)
                    int totalSeconds = designTime + (voteTime * players.size());

                    // 3. Thêm tổng thời gian vào beginDate
                    calendar.add(Calendar.SECOND, totalSeconds); // <-- Tính bằng GIÂY

                    // 4. Format lại thành chuỗi (luôn là format "simple")
                    synchronized (simpleFormatter) {
                        String endDate = simpleFormatter.format(calendar.getTime());
                        callback.onSuccess(endDate);
                    }
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