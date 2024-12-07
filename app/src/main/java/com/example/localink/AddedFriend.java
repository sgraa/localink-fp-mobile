package com.example.localink;

public class AddedFriend {
    private User user;
    private boolean hasActiveStory;

    // Required empty constructor for Firestore
    public AddedFriend() {}

    public AddedFriend(User user, boolean hasActiveStory) {
        this.user = user;
        this.hasActiveStory = hasActiveStory;
    }

    public User getUser() {
        return user;
    }

    public boolean hasActiveStory() {
        return hasActiveStory;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setHasActiveStory(boolean hasActiveStory) {
        this.hasActiveStory = hasActiveStory;
    }
}
