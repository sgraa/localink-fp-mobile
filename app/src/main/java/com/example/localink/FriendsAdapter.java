// File: FriendsAdapter.java
package com.example.localink;

import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localink.databinding.ItemFriendBinding;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * FriendsAdapter handles displaying search results (List<User>) in the RecyclerView.
 */
public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<User> friendsList;               // List of User from search results
    private List<AddedFriend> addedFriendsList;   // List of already added friends
    private OnFriendClickListener listener;

    /**
     * Constructor for FriendsAdapter.
     *
     * @param friendsList       List of users from search results.
     * @param addedFriendsList  List of already added friends to check if a user is already a friend.
     * @param listener          Listener for click events.
     */
    public FriendsAdapter(List<User> friendsList, List<AddedFriend> addedFriendsList, OnFriendClickListener listener) {
        this.friendsList = friendsList;
        this.addedFriendsList = addedFriendsList;
        this.listener = listener;
    }


    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout using View Binding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFriendBinding binding = ItemFriendBinding.inflate(inflater, parent, false);
        return new FriendViewHolder(binding);
    }


    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = friendsList.get(position);
        holder.bind(user, listener);

        // Show or hide the "Add Friend" button based on whether the user is already a friend
        if (isFriendAdded(user.getUid())) {
            holder.binding.addFriendButton.setVisibility(View.GONE);  // Hide if already added
        } else {
            holder.binding.addFriendButton.setVisibility(View.VISIBLE);  // Show if not added
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();  // Return the size of the friends list
    }

    /**
     * ViewHolder class to hold the view references using View Binding.
     */
    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendBinding binding;  // View Binding instance for item_friend.xml

        public FriendViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the User data to the views.
         *
         * @param user     The User object to bind.
         * @param listener The click listener.
         */
        public void bind(User user, OnFriendClickListener listener) {
            // Handle possible null or empty username
            String username = user.getUsername();
            if (username == null || username.isEmpty()) {
                username = user.getUid();  // Fallback to UID if username is missing
                Log.w("FriendsAdapter", "Username missing for UID: " + user.getUid());
            }
            binding.friendUsername.setText(username);

            // Load the profile picture using Glide with placeholder and error images
            Glide.with(binding.friendProfilePic.getContext())
                    .load(user.getPhotoUrl())  // URL or resource ID
                    .placeholder(R.drawable.ic_profile_placeholder) // Default placeholder
                    .error(R.drawable.ic_profile_placeholder)       // Placeholder on error
                    .circleCrop()  // Apply circular crop transformation
                    .into(binding.friendProfilePic);

            // Set click listener on the friend item
            binding.getRoot().setOnClickListener(v -> listener.onFriendClick(user));

            // Set click listener on the Add Friend button
            binding.addFriendButton.setOnClickListener(v -> listener.onAddFriendClick(user, binding.addFriendButton));
        }
    }

    /**
     * Checks if a friend is already added.
     *
     * @param friendId The UID of the friend to check.
     * @return True if the friend is already added, false otherwise.
     */
    private boolean isFriendAdded(String friendId) {
        Log.d("FriendsAdapter", "Checking friendId: " + friendId);
        for (AddedFriend addedFriend : addedFriendsList) {
            Log.d("FriendsAdapter", "Comparing with added friend: " + addedFriend.getUser().getUid());
            if (addedFriend.getUser().getUid().equals(friendId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interface for handling friend item click events.
     */
    public interface OnFriendClickListener {
        void onFriendClick(User user);
        void onAddFriendClick(User user, View button);
    }
}
