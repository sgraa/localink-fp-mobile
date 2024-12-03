package com.example.localink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeMessage;
    private Button storiesButton, friendsButton, logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        welcomeMessage = findViewById(R.id.welcomeMessage);
        storiesButton = findViewById(R.id.storiesButton);
        friendsButton = findViewById(R.id.friendsButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Fetch the current user's username from Firestore
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        welcomeMessage.setText("Welcome, " + username);
                    } else {
                        welcomeMessage.setText("Welcome, User");
                    }
                })
                .addOnFailureListener(e -> {
                    welcomeMessage.setText("Welcome, User");
                });

        // Set onClickListener for Stories Button
        storiesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StoryActivity.class); // Navigate to Story Activity
            startActivity(intent);
        });

        // Set onClickListener for Friends Button
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FriendsActivity.class); // Navigate to Friends Activity
            startActivity(intent);
        });

        // Set onClickListener for Logout Button
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class); // Redirect to LoginActivity after logout
            startActivity(intent);
            finish();
        });
    }
}
