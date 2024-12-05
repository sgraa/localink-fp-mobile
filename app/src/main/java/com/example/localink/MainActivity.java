package com.example.localink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    private void uploadProfilePicture(Uri imageUri) {
        String userId = mAuth.getCurrentUser().getUid(); // Pastikan userId valid
        if (userId == null) {
            Log.e("Upload", "User ID is null");
            return; // Jangan lanjut jika userId null
        }

        StorageReference profileImageRef = storageRef.child("profile_pictures/" + userId + ".jpg");

        // Upload file ke Firebase Storage
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Dapatkan URL download setelah upload selesai
                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profileImageUrl = uri.toString();
                        Log.d("Upload", "Image uploaded successfully. URL: " + profileImageUrl);

                        // Simpan URL gambar di Firestore
                        db.collection("users").document(userId).update("profileImageUrl", profileImageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Profile image URL updated in Firestore");
                                    Glide.with(this).load(profileImageUrl).into(profileImageView); // Update ImageView
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to update Firestore", e));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Upload", "Image upload failed", e);
                    Glide.with(this).load(R.drawable.bijou_image).into(profileImageView); // Default image
                });
    }
}
