package com.example.localink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageButton;
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
    private ImageView profileImageView, navFriends, navHome, logoutButton;
    private ImageButton navStory;
    private TextView usernameTextView, friendCountTextView;
    private Button storiesButton, friendsButton;
    private static final int PICK_IMAGE_REQUEST = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView locationTextView;

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
        locationTextView = findViewById(R.id.locationTextView);

        // Initialize Navbar components
        navHome = findViewById(R.id.nav_home);
        navFriends = findViewById(R.id.nav_friends);
        navStory = findViewById(R.id.nav_story);

        // Set up Navbar navigation
        Navbar navbar = new Navbar(this);
        navbar.setupNavigation(navHome, navFriends, navStory);

        // Get current user's ID
        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        // Fetch user data from Firestore
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String photoUrl = documentSnapshot.getString("photoUrl");  // Changed to match database field

                // Set username
                usernameTextView.setText(username);

                // Load profile image if available, otherwise use default
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this).load(photoUrl).into(profileImageView);
                } else {
                    Glide.with(this).load(R.drawable.bijou_image).into(profileImageView);
                }
            } else {
                // If the document does not exist, set default image
                Glide.with(this).load(R.drawable.bijou_image).into(profileImageView);
            }
        });

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if the location permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            // Request permission if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

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

        // Initialize userstoriesButton
        Button userstoriesButton = findViewById(R.id.userstoriesButton);
        userstoriesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UserStoryActivity.class);
            startActivity(intent);
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

        Button myfriendsButton = findViewById(R.id.myfriendsButton);
        myfriendsButton.setOnClickListener(v -> {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            locationTextView.setText("Location: Permission denied");
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            String cityCountry = getCityCountry(location);
                            locationTextView.setText(cityCountry);
                        } else {
                            locationTextView.setText("Location: Unknown");
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        locationTextView.setText("Location: Error retrieving location");
                        Log.e("LocationError", "Failed to get location", e);
                    });
        }
    }

    private String getCityCountry(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            String addressLine = address.getAddressLine(0);

            StringBuilder locationDetails = new StringBuilder();
            if (addressLine != null) locationDetails.append(addressLine).append(", ");

            return locationDetails.toString();
        } else {
            return "Unknown location";
        }
    }

    private void uploadProfilePicture(Uri imageUri) {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) {
            Log.e("Upload", "User ID is null");
            return;
        }

        StorageReference profileImageRef = storageRef.child("profile_pictures/" + userId + ".jpg");

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String photoUrl = uri.toString();  // Changed variable name to match database field
                        Log.d("Upload", "Image uploaded successfully. URL: " + photoUrl);

                        // Update to use photoUrl field name
                        db.collection("users").document(userId).update("photoUrl", photoUrl)  // Changed to match database field
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "Profile image URL updated in Firestore");
                                    Glide.with(this).load(photoUrl).into(profileImageView);
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to update Firestore", e));
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Upload", "Image upload failed", e);
                    Glide.with(this).load(R.drawable.bijou_image).into(profileImageView);
                });
    }
}