package com.ganshapebattle.models;

import android.graphics.Bitmap;
import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;
import java.util.List;

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

    // --- Additional Methods ---

    /**
     * Chuyển đổi avatar từ chuỗi Base64 thành Bitmap
     * @return Bitmap của avatar, hoặc null nếu có lỗi
     */
    public Bitmap getBitMapAvatar() {
        if (avatar == null || avatar.isEmpty()) {
            return null;
        }
        return ImageUtils.base64ToBitmap(avatar);
    }

    /**
     * Trả về chuỗi Base64 của avatar
     * @return Chuỗi Base64 của avatar
     */
    public String getStringBase64Avatar() {
        return avatar;
    }

    /**
     * Lấy danh sách pictures của user này
     * @param callback Callback để xử lý kết quả
     */
    public void getPictures(SupabaseCallback<List<Picture>> callback) {
        PictureService pictureService = new PictureService();
        // Lấy tất cả pictures có username = this.username
        pictureService.getAllPictures(new SupabaseCallback<List<Picture>>() {
            @Override
            public void onSuccess(List<Picture> allPictures) {
                // Lọc ra chỉ những pictures của user này
                List<Picture> userPictures = allPictures.stream()
                    .filter(picture -> username.equals(picture.getUsername()))
                    .collect(java.util.stream.Collectors.toList());
                callback.onSuccess(userPictures);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Lấy danh sách players của user này
     * @param callback Callback để xử lý kết quả
     */
    public void getPlayers(SupabaseCallback<List<Player>> callback) {
        PlayerService playerService = new PlayerService();
        // Lấy tất cả players có username = this.username
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                // Lọc ra chỉ những players của user này
                List<Player> userPlayers = allPlayers.stream()
                    .filter(player -> username.equals(player.getUsername()))
                    .collect(java.util.stream.Collectors.toList());
                callback.onSuccess(userPlayers);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Lấy danh sách lobbies mà user này tham gia với vai trò player
     * @param callback Callback để xử lý kết quả
     */
    public void getLobbiesAsPlayer(SupabaseCallback<List<Lobby>> callback) {
        PlayerService playerService = new PlayerService();
        LobbyService lobbyService = new LobbyService();
        
        // Lấy tất cả players của user này
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                List<Player> userPlayers = allPlayers.stream()
                    .filter(player -> username.equals(player.getUsername()))
                    .collect(java.util.stream.Collectors.toList());
                
                if (userPlayers.isEmpty()) {
                    callback.onSuccess(java.util.Collections.emptyList());
                    return;
                }
                
                // Lấy danh sách lobby IDs
                List<String> lobbyIds = userPlayers.stream()
                    .map(Player::getLobbyId)
                    .collect(java.util.stream.Collectors.toList());
                
                // Lấy thông tin lobbies
                lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
                    @Override
                    public void onSuccess(List<Lobby> allLobbies) {
                        List<Lobby> userLobbies = allLobbies.stream()
                            .filter(lobby -> lobbyIds.contains(lobby.getId()))
                            .collect(java.util.stream.Collectors.toList());
                        callback.onSuccess(userLobbies);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Lấy danh sách lobbies mà user này là admin
     * @param callback Callback để xử lý kết quả
     */
    public void getLobbiesAsAdmin(SupabaseCallback<List<Lobby>> callback) {
        LobbyService lobbyService = new LobbyService();
        // Lấy tất cả lobbies có admin = this.username
        lobbyService.getAllLobbies(new SupabaseCallback<List<Lobby>>() {
            @Override
            public void onSuccess(List<Lobby> allLobbies) {
                List<Lobby> adminLobbies = allLobbies.stream()
                    .filter(lobby -> username.equals(lobby.getAdminUsername()))
                    .collect(java.util.stream.Collectors.toList());
                callback.onSuccess(adminLobbies);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Lấy danh sách galleries của user này
     * @param callback Callback để xử lý kết quả
     */
    public void getGalleries(SupabaseCallback<List<Gallery>> callback) {
        GalleryService galleryService = new GalleryService();
        // Lấy tất cả galleries (có thể cần filter theo user nếu có field user trong Gallery)
        galleryService.getAllGalleries(callback);
    }
}