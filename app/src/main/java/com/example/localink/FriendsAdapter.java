// File: FriendsAdapter.java
package com.example.localink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.localink.databinding.ItemFriendBinding;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * FriendsAdapter handles displaying search results (List<User>) in the RecyclerView.
 */
public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<User> friendsList;  // List of User
    private List<AddedFriend> addedFriendsList;  // List of added friends
    private OnFriendClickListener listener;

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
            binding.friendUsername.setText(user.getUsername());

            // Load the profile picture using Glide with placeholder and error images
            Glide.with(binding.friendProfilePic.getContext())
                    .load(user.getProfilePicture())  // URL or resource ID
                    .placeholder(R.drawable.ic_profile_placeholder) // Default placeholder
                    .error(R.drawable.ic_profile_placeholder)       // Placeholder on error
                    .circleCrop()  // Apply circular crop transformation
                    .into(binding.friendProfilePic);

            // Set click listener on the friend item
            binding.getRoot().setOnClickListener(v -> listener.onFriendClick(user));
            binding.addFriendButton.setOnClickListener(v -> listener.onAddFriendClick(user));
        }
    }

    private boolean isFriendAdded(String friendId) {
        for (AddedFriend addedFriend : addedFriendsList) {
            if (addedFriend.getUser().getUid().equals(friendId)) {
                return true;  // Friend already added
            }
        }
        return false;  // Friend not added
    }

    /**
     * Interface for handling friend item click events.
     */
    public interface OnFriendClickListener {
        void onFriendClick(User user);
        void onAddFriendClick (User user);// Callback method when a friend item is clicked
    }
}