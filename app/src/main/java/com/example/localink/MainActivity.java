package com.example.localink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ImageView profileImageView;
    private TextView usernameTextView, friendCountTextView;
    private Button storiesButton, friendsButton, logoutButton;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize UI components
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        friendCountTextView = findViewById(R.id.friendCountTextView);
        storiesButton = findViewById(R.id.storiesButton);
        friendsButton = findViewById(R.id.friendsButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Get current user's ID
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // Fetch user data from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                // Set username
                usernameTextView.setText(username);

                // Load profile image if available, otherwise use default
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this).load(profileImageUrl).into(profileImageView);
                } else {
                    Glide.with(this).load(R.drawable.bijou_image).into(profileImageView); // Default profile picture
                }
            } else {
                // If the document does not exist, set default image
                Glide.with(this).load(R.drawable.bijou_image).into(profileImageView);
            }
        });

        // Fetch the number of friends for the current user
        userRef.collection("friends").get()
                .addOnSuccessListener(querySnapshot -> {
                    int friendCount = querySnapshot.size();
                    friendCountTextView.setText("Friends: " + friendCount);
                });

        // On click to change profile picture
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        // Navigate to stories screen
        storiesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StoryActivity.class);
            startActivity(intent);
        });

        // Navigate to friends screen
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FriendsActivity.class);
            startActivity(intent);
        });

        // Handle logout action
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadProfilePicture(imageUri);
        }
    }

    // Upload the selected profile picture to Firebase Storage
    private void uploadProfilePicture(Uri imageUri) {
        String userId = mAuth.getCurrentUser().getUid();
        StorageReference profileImageRef = storageRef.child("profile_pictures/" + userId + ".jpg");

        // Upload image to Firebase Storage
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // After upload, get the image URL
                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profileImageUrl = uri.toString();
                        // Update Firestore with the new image URL
                        db.collection("users").document(userId).update("profileImageUrl", profileImageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    // Update ImageView with the new profile picture
                                    Glide.with(this).load(profileImageUrl).into(profileImageView);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle upload failure (e.g., show error message)
                    Glide.with(this).load(R.drawable.bijou_image).into(profileImageView); // Fall back to default image
                });
    }
}
