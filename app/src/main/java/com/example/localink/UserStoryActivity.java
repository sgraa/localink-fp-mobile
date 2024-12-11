package com.example.localink;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class UserStoryActivity extends AppCompatActivity {

    private ViewPager2 storyViewPager;
    private FirebaseFirestore firestore;
    private StoryAdapter storyAdapter;
    private List<Media> mediaList;
    private ImageButton closeStoryButton;
    private ListenerRegistration storiesListener;

    private Handler handler = new Handler();
    private Runnable advanceStoryRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_story);

        storyViewPager = findViewById(R.id.storyViewPager);
        closeStoryButton = findViewById(R.id.closeStoryButton);
        firestore = FirebaseFirestore.getInstance();
        mediaList = new ArrayList<>();

        // Initialize the adapter
        storyAdapter = new StoryAdapter(mediaList, this);
        storyViewPager.setAdapter(storyAdapter);

        // Get the current user's ID and check authentication
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            // Fetch and display stories of the user's friends with real-time updates
            loadFriendsStories(userId);
        } else {
            Toast.makeText(this, "No story to display.", Toast.LENGTH_SHORT).show();
            finish();
            return; // Ensure that no further code is executed if user is not authenticated
        }

        // Initialize the advance runnable
        advanceStoryRunnable = () -> {
            if (!mediaList.isEmpty()) {
                int nextItem = (storyViewPager.getCurrentItem() + 1) % mediaList.size();
                storyViewPager.setCurrentItem(nextItem, true);
                handler.postDelayed(advanceStoryRunnable, 5000); // Advance every 5 seconds
            }
        };

        closeStoryButton.setOnClickListener(v -> {
            // Close the activity when close button is pressed
            finish();
        });
    }

    private void loadFriendsStories(String userId) {
        // Get the list of friends for the current user
        firestore.collection("users")
                .document(userId)
                .collection("friends")  // Assuming a collection of friends
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        // Iterate through friends and fetch their stories
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String friendId = document.getId();
                            loadUserStories(friendId);
                        }
                    } else {
                        Toast.makeText(this, "You have no friends.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching friends.", Toast.LENGTH_SHORT).show();
                    Log.e("UserStoryActivity", "Error fetching friends.", e);
                    finish();
                });
    }

    private void loadUserStories(String userId) {
        // Fetch active stories for the given user (friend)
        firestore.collection("users")
                .document(userId)
                .collection("stories")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .orderBy("createdAt", Query.Direction.ASCENDING) // Ensure ordering by createdAt
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error fetching user's stories.", Toast.LENGTH_SHORT).show();
                        Log.e("UserStoryActivity", "Error fetching user's stories.", e);
                        return;
                    }

                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        return; // No stories for this friend, continue with the next friend
                    }

                    long currentTime = System.currentTimeMillis();

                    // Add the stories to the mediaList
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        Media media = documentSnapshot.toObject(Media.class);
                        if (media != null) {
                            media.setMediaId(documentSnapshot.getId());
                            // Check if the story has not expired
                            if (media.getExpiresAt() > currentTime) {
                                mediaList.add(media);
                            } else {
                                // Optionally, delete expired stories
                                deleteExpiredStory(userId, documentSnapshot.getId());
                            }
                        }
                    }

                    // Notify adapter to display stories
                    storyAdapter.notifyDataSetChanged();

                    // Start auto-advance
                    handler.removeCallbacks(advanceStoryRunnable);
                    handler.postDelayed(advanceStoryRunnable, 5000); // Start after 5 seconds
                });
    }

    private void deleteExpiredStory(String userId, String storyId) {
        firestore.collection("users").document(userId).collection("stories").document(storyId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("UserStoryActivity", "Expired story deleted: " + storyId))
                .addOnFailureListener(e -> Log.e("UserStoryActivity", "Error deleting expired story: " + storyId, e));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start the auto-advance when the activity is visible
        handler.postDelayed(advanceStoryRunnable, 5000); // Advance every 5 seconds
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove callbacks when the activity is no longer visible
        handler.removeCallbacks(advanceStoryRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storiesListener != null) {
            storiesListener.remove();
        }
    }
}
