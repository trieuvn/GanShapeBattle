package com.ganshapebattle.models;

import com.google.gson.annotations.SerializedName;

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
}