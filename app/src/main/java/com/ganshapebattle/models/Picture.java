package com.ganshapebattle.models;

import android.graphics.Bitmap;
import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;

public class Picture {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("createdDate")
    private String createdDate;

    @SerializedName("type")
    private String type;

    @SerializedName("image")
    private String image;

    @SerializedName("tags")
    private String tags;

    @SerializedName("gallery")
    private String galleryId;

    @SerializedName("user")
    private String username;

    // Constructors
    public Picture() {
    }

    public Picture(String id, String name, String description, String createdDate, String type, String image, String tags, String galleryId, String username) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdDate = createdDate;
        this.type = type;
        this.image = image;
        this.tags = tags;
        this.galleryId = galleryId;
        this.username = username;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getGalleryId() { return galleryId; }
    public void setGalleryId(String galleryId) { this.galleryId = galleryId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    // --- Additional Methods ---

    /**
     * Lấy thông tin user của picture này
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
     * Lấy thông tin gallery của picture này
     * @param callback Callback để xử lý kết quả
     */
    public void getGallery(SupabaseCallback<Gallery> callback) {
        if (galleryId == null || galleryId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        
        GalleryService galleryService = new GalleryService();
        // Lấy gallery có id = this.galleryId
        galleryService.getGalleryById(galleryId, callback);
    }

    /**
     * Chuyển đổi image từ chuỗi Base64 thành Bitmap
     * @return Bitmap của image, hoặc null nếu có lỗi
     */
    public Bitmap getBitMapImage() {
        if (image == null || image.isEmpty()) {
            return null;
        }
        return ImageUtils.base64ToBitmap(image);
    }

    /**
     * Trả về chuỗi Base64 của image
     * @return Chuỗi Base64 của image
     */
    public String getStringBase64Image() {
        return image;
    }
}