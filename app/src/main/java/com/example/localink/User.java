// File: User.java
package com.example.localink;

/**
 * Represents a user in the application.
 */
public class User {
    private String uid;
    private String username;
    private String email;
    private String photoUrl;

    // Public no-argument constructor required for Firestore
    public User() {}

    // Parameterized constructor
    public User(String uid, String username, String email, String photoUrl) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
