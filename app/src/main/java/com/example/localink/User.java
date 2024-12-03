package com.example.localink;

public class User {
    private String username;
    private String profilePicture;
    private String uid;  // Added field for user ID
    private String email;

    // Default constructor required for Firestore
    public User() {}

    // Constructor to initialize the fields
    public User(String username, String profilePicture, String uid) {
        this.username = username;
        this.profilePicture = profilePicture;
        this.uid = uid;
        this.email = email;
    }

    // Getter for username
    public String getUsername() {
        return username;
    }

    // Setter for username
    public void setUsername(String username) {
        this.username = username;
    }

    // Getter for profile picture
    public String getProfilePicture() {
        return profilePicture;
    }

    // Setter for profile picture
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getEmail() {
        return email;  // Return the email if needed
    }

    public void setEmail(String email) {
        this.email = email;  // Setter for email
    }

    // Getter for user ID (uid)
    public String getUid() {
        return uid;
    }

    // Setter for user ID (uid)
    public void setUid(String uid) {
        this.uid = uid;
    }

    // Optionally, override toString() to make debugging easier
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", uid='" + uid + '\'' +
                '}';
    }
}
