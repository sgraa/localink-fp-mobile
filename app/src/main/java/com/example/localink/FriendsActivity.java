// File: FriendsActivity.java
package com.example.localink;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.localink.databinding.ActivityFriendsBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendsActivity handles displaying both the search results for adding new friends
 * and the list of already added friends with indicators for active stories.
 */
public class FriendsActivity extends AppCompatActivity implements
        FriendsAdapter.OnFriendClickListener,
        AddedFriendsAdapter.OnFriendClickListener {

    private ActivityFriendsBinding binding;             // View Binding instance for activity_friends.xml
    private FriendsAdapter friendsAdapter;               // Adapter for search results
    private AddedFriendsAdapter addedFriendsAdapter;     // Adapter for added friends
    private List<User> friendsList;                      // List of users from search
    private List<AddedFriend> addedFriendsList;           // List of added friends with story status
    private FirebaseFirestore db;                        // Firestore instance
    private FirebaseAuth auth;                           // FirebaseAuth instance
    private FirebaseUser currentUser;                    // Current authenticated user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using View Binding
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            // Not signed in, redirect to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Set LayoutManagers for RecyclerViews
        binding.friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.addedFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data lists
        friendsList = new ArrayList<>();
        addedFriendsList = new ArrayList<>();

        // Initialize and set Adapters
        friendsAdapter = new FriendsAdapter(friendsList, addedFriendsList, this);
        binding.friendsRecyclerView.setAdapter(friendsAdapter);

        addedFriendsAdapter = new AddedFriendsAdapter(addedFriendsList, this);
        binding.addedFriendsRecyclerView.setAdapter(addedFriendsAdapter);

        // Load already added friends with their story status
        loadAddedFriends();

        // Set up search functionality
        setupSearchFunctionality();

        // Initialize and setup the Navbar
        ImageView navHome = findViewById(R.id.nav_home);
        ImageView navFriends = findViewById(R.id.nav_friends);
        ImageButton navStory = findViewById(R.id.nav_story);

        // Set up the Navbar (Assuming Navbar is a custom class handling navigation)
        Navbar navbar = new Navbar(FriendsActivity.this);
        navbar.setupNavigation(navHome, navFriends, navStory);
    }

    /**
     * Sets up the text watcher for the search EditText to filter friends based on input.
     */
    private void setupSearchFunctionality() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterFriendsList(charSequence.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Not used
            }
        });
    }

    /**
     * Loads the list of added friends from Firestore and checks for active stories.
     */
    private void loadAddedFriends() {
        String userId = currentUser.getUid();
        CollectionReference friendsRef = db.collection("users").document(userId).collection("friends");

        friendsRef.get().addOnSuccessListener(querySnapshot -> {
            addedFriendsList.clear();
            List<Task<?>> tasks = new ArrayList<>();

            for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    user.setUid(documentSnapshot.getId());  // Set the UID manually

                    // Check if the friend has an active story
                    Task<QuerySnapshot> task = db.collection("users")
                            .document(user.getUid())
                            .collection("stories")
                            .whereGreaterThan("expiresAt", System.currentTimeMillis())
                            .get()
                            .addOnSuccessListener(storyQuerySnapshot -> {
                                boolean hasActiveStory = !storyQuerySnapshot.isEmpty();
                                addedFriendsList.add(new AddedFriend(user, hasActiveStory));
                            })
                            .addOnFailureListener(e -> {
                                // Assume no active story if an error occurs
                                addedFriendsList.add(new AddedFriend(user, false));
                            });

                    tasks.add(task);
                }
            }

            // Wait for all story checks to complete before updating the adapter
            Tasks.whenAllComplete(tasks)
                    .addOnCompleteListener(task -> {
                        addedFriendsAdapter.notifyDataSetChanged();
                        friendsAdapter.notifyDataSetChanged();
                    });
        }).addOnFailureListener(e -> {
            // Handle failure silently, without showing error alerts
        });
    }

    /**
     * Filters the friends list based on the search query.
     *
     * @param query The search query entered by the user.
     */
    private void filterFriendsList(String query) {
        if (query.isEmpty()) {
            // If search query is empty, clear the search results
            friendsList.clear();
            friendsAdapter.notifyDataSetChanged();
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users")
                .whereEqualTo("username", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendsList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                user.setUid(doc.getId());  // Set the UID manually
                                if (!user.getUid().equals(userId) && !isFriendAdded(user.getUid())) {
                                    friendsList.add(user);
                                }
                            }
                        }
                        friendsAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure silently, without showing error alerts
                });
    }

    /**
     * Checks if a user is already added as a friend.
     *
     * @param friendId The UID of the friend to check.
     * @return True if the user is already added as a friend, false otherwise.
     */
    private boolean isFriendAdded(String friendId) {
        for (AddedFriend addedFriend : addedFriendsList) {
            if (addedFriend.getUser().getUid().equals(friendId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles click events from the FriendsAdapter (search results).
     *
     * @param user The User object that was clicked.
     */
    @Override
    public void onFriendClick(User user) {
        // Check if the user is in the added friends list
        for (AddedFriend addedFriend : addedFriendsList) {
            if (addedFriend.getUser().getUid().equals(user.getUid())) {
                if (addedFriend.hasActiveStory()) {
                    // Open StoryViewerActivity to view friend's story
                    Intent intent = new Intent(FriendsActivity.this, StoryViewerActivity.class);
                    intent.putExtra("friendUserId", user.getUid());
                    startActivity(intent);
                }
                return; // Exit after handling
            }
        }

        // If the user is not in the added friends list, you might want to add them
        // For example, prompt to add as a friend
    }

    /**
     * Handles click events from the AddedFriendsAdapter (added friends list).
     *
     * @param addedFriend The AddedFriend object that was clicked.
     */
    @Override
    public void onFriendClick(AddedFriend addedFriend) {
        User user = addedFriend.getUser();
        if (addedFriend.hasActiveStory()) {
            // Open StoryViewerActivity to view friend's story
            Intent intent = new Intent(FriendsActivity.this, StoryViewerActivity.class);
            intent.putExtra("friendUserId", user.getUid());
            startActivity(intent);
        }
    }

    /**
     * Adds a new friend to the Firestore collection ensuring mutual friendship.
     *
     * @param user   The User object to add as a friend.
     * @param button The button view that was clicked (for potential UI feedback).
     */
    @Override
    public void onAddFriendClick(User user, View button) {
        String currentUserId = currentUser.getUid();

        // Fetch the current user's username from Firestore
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(document -> {
                    String currentUsername = "Unknown";
                    String currentEmail = currentUser.getEmail();
                    String currentPhotoUrl = (currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : null;

                    if (document.exists()) {
                        String fetchedUsername = document.getString("username");
                        if (fetchedUsername != null && !fetchedUsername.isEmpty()) {
                            currentUsername = fetchedUsername;
                        }

                        String fetchedEmail = document.getString("email");
                        if (fetchedEmail != null && !fetchedEmail.isEmpty()) {
                            currentEmail = fetchedEmail;
                        }

                        String fetchedPhotoUrl = document.getString("photoUrl");
                        if (fetchedPhotoUrl != null && !fetchedPhotoUrl.isEmpty()) {
                            currentPhotoUrl = fetchedPhotoUrl;
                        }
                    }

                    addFriendWithUserData(user, currentUserId, currentUsername, currentEmail, currentPhotoUrl);
                });
    }

    private void addFriendWithUserData(User friendToAdd, String currentUserId, String currentUsername, String currentEmail, String currentPhotoUrl) {
        CollectionReference currentUserFriendsRef = db.collection("users")
                .document(currentUserId)
                .collection("friends");

        CollectionReference selectedUserFriendsRef = db.collection("users")
                .document(friendToAdd.getUid())
                .collection("friends");

        WriteBatch batch = db.batch();

        // Add selected user to current user's friends
        User friendUser = new User(
                friendToAdd.getUid(),
                friendToAdd.getUsername(),
                friendToAdd.getEmail(),
                friendToAdd.getPhotoUrl()
        );
        DocumentReference currentUserFriendDoc = currentUserFriendsRef.document(friendToAdd.getUid());
        batch.set(currentUserFriendDoc, friendUser);

        // Create a User object for the current user to add to the selected friend's collection
        User currentUserObj = new User(
                currentUserId,
                currentUsername,
                currentEmail,
                currentPhotoUrl
        );

        DocumentReference selectedUserFriendDoc = selectedUserFriendsRef.document(currentUserId);
        batch.set(selectedUserFriendDoc, currentUserObj);

        // Commit the batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Add the friend to addedFriendsList immediately
                    addedFriendsList.add(new AddedFriend(friendToAdd, false));

                    // Remove from search results
                    friendsList.remove(friendToAdd);

                    // Update both adapters
                    friendsAdapter.notifyDataSetChanged();
                    addedFriendsAdapter.notifyDataSetChanged();

                    // Load added friends to check for stories
                    loadAddedFriends();
                });
    }
}
