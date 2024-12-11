// File: AddedFriendsAdapter.java
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
 * AddedFriendsAdapter handles displaying added friends (List<AddedFriend>) in the RecyclerView.
 */
public class AddedFriendsAdapter extends RecyclerView.Adapter<AddedFriendsAdapter.AddedFriendViewHolder> {

    private List<AddedFriend> addedFriendsList;  // List of AddedFriend
    private OnFriendClickListener listener;

    /**
     * Constructor for AddedFriendsAdapter.
     *
     * @param addedFriendsList The list of added friends to display.
     * @param listener         The listener for click events.
     */
    public AddedFriendsAdapter(List<AddedFriend> addedFriendsList, OnFriendClickListener listener) {
        this.addedFriendsList = addedFriendsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddedFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout using View Binding
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemFriendBinding binding = ItemFriendBinding.inflate(inflater, parent, false);
        return new AddedFriendViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddedFriendViewHolder holder, int position) {
        AddedFriend addedFriend = addedFriendsList.get(position);
        holder.bind(addedFriend, listener);
    }

    @Override
    public int getItemCount() {
        return addedFriendsList.size();  // Return the size of the addedFriendsList
    }

    /**
     * ViewHolder class to hold the view references using View Binding.
     */
    public static class AddedFriendViewHolder extends RecyclerView.ViewHolder {
        private final ItemFriendBinding binding;  // View Binding instance for item_friend.xml

        public AddedFriendViewHolder(ItemFriendBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the AddedFriend data to the views.
         *
         * @param addedFriend The AddedFriend object to bind.
         * @param listener    The click listener.
         */
        public void bind(AddedFriend addedFriend, OnFriendClickListener listener) {
            User user = addedFriend.getUser();

            // Handle possible null or empty username
            String username = user.getUsername();
            if (username == null || username.isEmpty()) {
                username = user.getUid(); // Fallback to UID if username is missing
                Log.w("AddedFriendsAdapter", "Username missing for UID: " + user.getUid());
            }
            binding.friendUsername.setText(username);

            // Hide the Add Friend button since these are already friends
            binding.addFriendButton.setVisibility(View.GONE);

            // Load the profile picture using Glide with placeholder and error images
            String profileImageUrl = user.getPhotoUrl(); // Make sure this matches your User class getter
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(binding.friendProfilePic.getContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.friendProfilePic);
            } else {
                binding.friendProfilePic.setImageResource(R.drawable.ic_profile_placeholder);
            }

            if (addedFriend.hasActiveStory()) {
                binding.storyIndicator.setVisibility(View.VISIBLE);
            } else {
                binding.storyIndicator.setVisibility(View.GONE);
            }

            // Set click listener on the friend item
            binding.getRoot().setOnClickListener(v -> listener.onFriendClick(addedFriend));
        }
    }

    /**
     * Interface for handling added friend item click events.
     */
    public interface OnFriendClickListener {
        void onFriendClick(AddedFriend addedFriend);
    }
}
