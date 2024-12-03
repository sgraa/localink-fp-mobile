package com.example.localink;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity implements FriendsAdapter.OnFriendClickListener {

    private RecyclerView recyclerView;
    private RecyclerView addedFriendsRecyclerView;
    private FriendsAdapter adapter;
    private List<User> friendsList, addedFriendsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        recyclerView = findViewById(R.id.friendsRecyclerView);
        addedFriendsRecyclerView = findViewById(R.id.addedFriendsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        addedFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        friendsList = new ArrayList<>();
        addedFriendsList = new ArrayList<>();

        adapter = new FriendsAdapter(friendsList, this);
        recyclerView.setAdapter(adapter);

        // Initialize added friends
        loadAddedFriends();

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}


            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                filterFriendsList(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void loadAddedFriends() {
        String userId = auth.getCurrentUser().getUid();
        CollectionReference usersRef = db.collection("users").document(userId).collection("friends");

        usersRef.get().addOnSuccessListener(querySnapshot -> {
            addedFriendsList.clear();
            for (DocumentSnapshot documentSnapshot : querySnapshot) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    user.setUid(documentSnapshot.getId());  // Set the UID (document ID) manually
                    addedFriendsList.add(user);
                }
            }

            // Set the adapter to the RecyclerView if it's not already set
            if (addedFriendsRecyclerView.getAdapter() == null) {
                FriendsAdapter addedFriendsAdapter = new FriendsAdapter(addedFriendsList, this);
                addedFriendsRecyclerView.setAdapter(addedFriendsAdapter);
            } else {
                addedFriendsRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> Toast.makeText(FriendsActivity.this, "Error loading friends", Toast.LENGTH_SHORT).show());
    }

    private void filterFriendsList(String query) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .whereEqualTo("username", query)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        friendsList.clear();
                        for (DocumentSnapshot document : result) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setUid(document.getId());  // Set the UID (document ID) manually
                                if (!user.getUid().equals(userId) && !addedFriendsList.contains(user)) {
                                    friendsList.add(user);
                                } else {
                                    Log.w("FriendsActivity", "User or UID is null for: " + document.getId());
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(FriendsActivity.this, "Error retrieving users", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FriendsActivity", "Error fetching users for search", e);
                    Toast.makeText(FriendsActivity.this, "Error retrieving users", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onFriendClick(User user) {
        // Get current user's UID
        String userId = auth.getCurrentUser().getUid();

        // Check if the friend is already added
        db.collection("users").document(userId)
                .collection("friends")
                .document(user.getUid()) // Document ID will be the friend's UID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot.exists()) {
                            // Friend is already added
                            Toast.makeText(FriendsActivity.this, "You've already added this friend.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Friend is not added yet, proceed with adding
                            addFriend(user, userId);
                        }
                    } else {
                        // Handle error retrieving friend data
                        Toast.makeText(FriendsActivity.this, "Error checking if friend is added", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addFriend(User user, String userId) {
        // Add user to friends collection
        db.collection("users").document(userId)
                .collection("friends").document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FriendsActivity.this, "Friend added!", Toast.LENGTH_SHORT).show();
                    loadAddedFriends(); // Reload added friends list
                })
                .addOnFailureListener(e -> Toast.makeText(FriendsActivity.this, "Failed to add friend", Toast.LENGTH_SHORT).show());
    }
}
