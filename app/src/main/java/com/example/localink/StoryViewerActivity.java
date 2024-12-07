// File: StoryViewerActivity.java
package com.example.localink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryViewerActivity extends AppCompatActivity {

    private ViewPager2 storyViewPager;
    private FirebaseFunctions functions;
    private FirebaseFirestore firestore;
    private StoryAdapter storyAdapter;
    private List<Media> mediaList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        storyViewPager = findViewById(R.id.storyViewPager);

        functions = FirebaseFunctions.getInstance();
        firestore = FirebaseFirestore.getInstance();

        mediaList = new ArrayList<>();

        // Initialize the adapter
        storyAdapter = new StoryAdapter(mediaList, this);
        storyViewPager.setAdapter(storyAdapter);

        // Get friendUserId from intent
        Intent intent = getIntent();
        String friendUserId = intent.getStringExtra("friendUserId");

        if (friendUserId != null) {
            // Fetch and display friend's active stories
            loadFriendStories(friendUserId);
        } else {
            Toast.makeText(this, "No story to display.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadFriendStories(String friendUserId) {
        // Fetch active stories for the friend
        firestore.collection("users")
                .document(friendUserId)
                .collection("stories")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No active stories for this friend.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        Media media = documentSnapshot.toObject(Media.class);
                        if (media != null) {
                            media.setMediaId(documentSnapshot.getId());
                            mediaList.add(media);
                        }
                    }

                    // Notify adapter to display stories
                    storyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching friend's stories.", Toast.LENGTH_SHORT).show();
                    Log.e("StoryViewerActivity", "Error fetching friend's stories.", e);
                    finish();
                });
    }
}
