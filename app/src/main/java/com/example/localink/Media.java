package com.example.localink;

public class Media {
    private String userId;
    private String downloadUrl;
    private String type; // "photo" or "video"
    private long createdAt;
    private String fileName;
    private long expiresAt;
    private String mediaId; // Firestore document ID

    // Default constructor required for Firestore
    public Media() {}

    // Parameterized constructor
    public Media(String userId, String downloadUrl, String type, long createdAt, String fileName, long expiresAt) {
        this.userId = userId;
        this.downloadUrl = downloadUrl;
        this.type = type;
        this.createdAt = createdAt;
        this.fileName = fileName;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }
}