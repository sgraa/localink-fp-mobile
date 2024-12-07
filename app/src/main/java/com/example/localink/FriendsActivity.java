// File: FriendsActivity.java
package com.example.localink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localink.databinding.ActivityFriendsBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendsActivity handles displaying both the search results for adding new friends
 * and the list of already added friends with indicators for active stories.
 */
public class FriendsActivity extends AppCompatActivity implements FriendsAdapter.OnFriendClickListener, AddedFriendsAdapter.OnFriendClickListener {

    private ActivityFriendsBinding binding;             // View Binding instance for activity_friends.xml
    private FriendsAdapter friendsAdapter;               // Adapter for search results
    private AddedFriendsAdapter addedFriendsAdapter;     // Adapter for added friends
    private List<User> friendsList;                       // List of users from search
    private List<AddedFriend> addedFriendsList;           // List of added friends with story status
    private FirebaseFirestore db;                         // Firestore instance
    private FirebaseAuth auth;                            // FirebaseAuth instance
    private FirebaseFunctions functions;                  // FirebaseFunctions instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using View Binding
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();

        // Set LayoutManagers for RecyclerViews
        binding.friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.addedFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize data lists
        friendsList = new ArrayList<>();
        addedFriendsList = new ArrayList<>();

        // Initialize and set Adapters
        friendsAdapter = new FriendsAdapter(friendsList, this);
        binding.friendsRecyclerView.setAdapter(friendsAdapter);

        addedFriendsAdapter = new AddedFriendsAdapter(addedFriendsList, this);
        binding.addedFriendsRecyclerView.setAdapter(addedFriendsAdapter);

        // Load already added friends with their story status
        loadAddedFriends();

        // Set up search functionality
        setupSearchFunctionality();
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
        String userId = auth.getCurrentUser().getUid();
        CollectionReference friendsRef = db.collection("users").document(userId).collection("friends");

        friendsRef.get().addOnSuccessListener(querySnapshot -> {
            addedFriendsList.clear();
            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

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
                                Log.e("FriendsActivity", "Error fetching stories for friend: " + user.getUsername(), e);
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

                        // Optionally, show a toast or log
                        Toast.makeText(FriendsActivity.this, "Added friends loaded with stories", Toast.LENGTH_SHORT).show();
                        Log.d("FriendsActivity", "Added friends loaded with stories.");
                    });
        }).addOnFailureListener(e -> {
            Log.e("FriendsActivity", "Error loading friends", e);
            Toast.makeText(FriendsActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show();
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

        String userId = auth.getCurrentUser().getUid();

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
                    } else {
                        Log.e("FriendsActivity", "Error retrieving users", task.getException());
                        Toast.makeText(FriendsActivity.this, "Error retrieving users", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsActivity", "Error fetching users for search", e);
                    Toast.makeText(FriendsActivity.this, "Error retrieving users", Toast.LENGTH_SHORT).show();
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
                } else {
                    Toast.makeText(this, "This friend has no active stories.", Toast.LENGTH_SHORT).show();
                }
                return; // Exit after handling
            }
        }

        // If the user is not in the added friends list, you might want to add them
        // For example, prompt to add as a friend
        Toast.makeText(this, "This user is not in your added friends list.", Toast.LENGTH_SHORT).show();
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
        } else {
            Toast.makeText(this, "This friend has no active stories.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adds a new friend to the Firestore collection.
     *
     * @param user   The User object to add as a friend.
     * @param userId The UID of the current user.
     */
    private void addFriend(User user, String userId) {
        // Add user to friends collection
        db.collection("users").document(userId)
                .collection("friends").document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FriendsActivity.this, "Friend added!", Toast.LENGTH_SHORT).show();
                    loadAddedFriends(); // Reload added friends list
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsActivity", "Failed to add friend: " + user.getUsername(), e);
                    Toast.makeText(FriendsActivity.this, "Failed to add friend", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Checks if a user is already added as a friend.
     * (Redundant method - can be removed if not needed elsewhere)
     *
     * @param friendId The UID of the friend to check.
     * @return True if already added, false otherwise.
     */
    private boolean isFriendAlreadyAdded(String friendId) {
        // This method is redundant as isFriendAdded covers the same functionality
        return isFriendAdded(friendId);
    }
}
