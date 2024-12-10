// File: AddedFriend.java
package com.example.localink;

/**
 * Represents an added friend along with their story status.
 */
public class AddedFriend {
    private User user;
    private boolean hasActiveStory;

    // Public no-argument constructor required for Firestore
    public AddedFriend() {}

    // Parameterized constructor
    public AddedFriend(User user, boolean hasActiveStory) {
        this.user = user;
        this.hasActiveStory = hasActiveStory;
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean hasActiveStory() {
        return hasActiveStory;
    }

    public void setHasActiveStory(boolean hasActiveStory) {
        this.hasActiveStory = hasActiveStory;
    }
}
