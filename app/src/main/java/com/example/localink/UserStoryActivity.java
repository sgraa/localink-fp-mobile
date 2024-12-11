package com.example.localink;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserStoryActivity extends AppCompatActivity {

    private RecyclerView storyRecyclerView;
    private UserStoryAdapter userStoryAdapter; // Updated Adapter Name
    private List<Media> mediaList;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_story);

        // Initialize views
        storyRecyclerView = findViewById(R.id.storyRecyclerView);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        mediaList = new ArrayList<>();

        // Initialize RecyclerView
        userStoryAdapter = new UserStoryAdapter(mediaList, this); // Updated Adapter Name
        storyRecyclerView.setAdapter(userStoryAdapter);
        storyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check authentication and load stories
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            loadFriendsStories(userId);
        } else {
            finish(); // If no user is authenticated, just finish the activity
        }
    }

    private void loadFriendsStories(String userId) {
        firestore.collection("users")
                .document(userId)
                .collection("friends")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        // Iterate through friends and fetch their stories
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String friendId = document.getId();
                            loadUserStories(friendId);
                        }
                    } else {
                        finish(); // If no friends are found, just finish the activity
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserStoryActivity", "Error fetching friends.", e);
                    finish(); // If error occurs, just finish the activity
                });
    }

    private void loadUserStories(String userId) {
        firestore.collection("users")
                .document(userId)
                .collection("stories")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("UserStoryActivity", "Error fetching user's stories.", e);
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        return; // No stories to display, do nothing
                    }

                    long currentTime = System.currentTimeMillis();

                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        Media media = documentSnapshot.toObject(Media.class);
                        if (media != null && media.getExpiresAt() > currentTime) {
                            mediaList.add(media);
                        } else {
                            deleteExpiredStory(userId, documentSnapshot.getId());
                        }
                    }

                    // Notify the adapter to update the UI
                    userStoryAdapter.notifyDataSetChanged(); // Updated Adapter Name
                });
    }

    private void deleteExpiredStory(String userId, String storyId) {
        firestore.collection("users").document(userId).collection("stories").document(storyId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("UserStoryActivity", "Expired story deleted: " + storyId))
                .addOnFailureListener(e -> Log.e("UserStoryActivity", "Error deleting expired story: " + storyId, e));
    }
}
