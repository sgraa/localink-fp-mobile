// File: StoryViewerActivity.java
package com.example.localink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StoryViewerActivity extends AppCompatActivity {

    private ViewPager2 storyViewPager;
    private FirebaseFirestore firestore;
    private StoryAdapter storyAdapter;
    private List<Media> mediaList;
    private ListenerRegistration storiesListener;

    private Handler handler = new Handler();
    private Runnable advanceStoryRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        storyViewPager = findViewById(R.id.storyViewPager);

        firestore = FirebaseFirestore.getInstance();

        mediaList = new ArrayList<>();

        // Initialize the adapter
        storyAdapter = new StoryAdapter(mediaList, this);
        storyViewPager.setAdapter(storyAdapter);

        // Get friendUserId from intent
        Intent intent = getIntent();
        String friendUserId = intent.getStringExtra("friendUserId");

        if (friendUserId != null) {
            // Fetch and display friend's active stories with real-time updates
            loadFriendStories(friendUserId);
        } else {
            Toast.makeText(this, "No story to display.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initialize the advance runnable
        advanceStoryRunnable = () -> {
            if (!mediaList.isEmpty()) {
                int nextItem = (storyViewPager.getCurrentItem() + 1) % mediaList.size();
                storyViewPager.setCurrentItem(nextItem, true);
                handler.postDelayed(advanceStoryRunnable, 5000); // Advance every 5 seconds
            }
        };
    }

    private void loadFriendStories(String friendUserId) {
        // Fetch active stories for the friend with real-time listener
        storiesListener = firestore.collection("users")
                .document(friendUserId)
                .collection("stories")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error fetching friend's stories.", Toast.LENGTH_SHORT).show();
                        Log.e("StoryViewerActivity", "Error fetching friend's stories.", e);
                        return;
                    }

                    if (querySnapshot == null) {
                        Toast.makeText(this, "No active stories to display.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mediaList.clear();
                    long currentTime = System.currentTimeMillis();

                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        Media media = documentSnapshot.toObject(Media.class);
                        if (media != null) {
                            media.setMediaId(documentSnapshot.getId());
                            // Check if the story has not expired
                            if (media.getExpiresAt() > currentTime) {
                                mediaList.add(media);
                            } else {
                                // Optionally, delete expired stories
                                deleteExpiredStory(friendUserId, documentSnapshot.getId());
                            }
                        }
                    }

                    if (mediaList.isEmpty()) {
                        Toast.makeText(this, "No active stories for this friend.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Notify adapter to display stories
                    storyAdapter.notifyDataSetChanged();

                    // Start auto-advance
                    handler.removeCallbacks(advanceStoryRunnable);
                    handler.postDelayed(advanceStoryRunnable, 5000); // Start after 5 seconds
                });
    }

    /**
     * Deletes an expired story from Firestore.
     *
     * @param userId  The ID of the user.
     * @param storyId The ID of the story to delete.
     */
    private void deleteExpiredStory(String userId, String storyId) {
        firestore.collection("users").document(userId).collection("stories").document(storyId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("StoryViewerActivity", "Expired story deleted: " + storyId))
                .addOnFailureListener(e -> Log.e("StoryViewerActivity", "Error deleting expired story: " + storyId, e));
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
