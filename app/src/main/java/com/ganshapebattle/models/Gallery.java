package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;
import com.ganshapebattle.services.*;
import com.ganshapebattle.services.SupabaseCallback;
import java.util.List;

public class Gallery {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    // Constructors
    public Gallery() {
    }

    public Gallery(String id, String name, String description, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // --- Additional Methods ---

    /**
     * Lấy danh sách pictures trong gallery này
     * @param callback Callback để xử lý kết quả
     */
    public void getPictures(SupabaseCallback<List<Picture>> callback) {
        PictureService pictureService = new PictureService();
        // Lấy tất cả pictures có galleryId = this.id
        pictureService.getAllPictures(new SupabaseCallback<List<Picture>>() {
            @Override
            public void onSuccess(List<Picture> allPictures) {
                // Lọc ra chỉ những pictures của gallery này
                List<Picture> galleryPictures = allPictures.stream()
                    .filter(picture -> id.equals(picture.getGalleryId()))
                    .collect(java.util.stream.Collectors.toList());
                callback.onSuccess(galleryPictures);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}