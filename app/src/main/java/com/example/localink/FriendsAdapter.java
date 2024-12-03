package com.example.localink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {

    private List<User> friendsList;  // List of friends
    private OnFriendClickListener listener;

    // Constructor for the adapter
    public FriendsAdapter(List<User> friendsList, OnFriendClickListener listener) {
        this.friendsList = friendsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = friendsList.get(position);
        holder.username.setText(user.getUsername());
        // Load the profile picture using Glide (you can replace with your own logic)
        Glide.with(holder.profilePic.getContext())
                .load(user.getProfilePicture())
                .circleCrop()
                .into(holder.profilePic);

        // Set click listener on the item
        holder.itemView.setOnClickListener(v -> listener.onFriendClick(user));
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    // ViewHolder class to hold the view references
    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView profilePic;

        public FriendViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.friend_username);
            profilePic = itemView.findViewById(R.id.friend_profile_pic);
        }
    }

    // Interface for item click listener
    public interface OnFriendClickListener {
        void onFriendClick(User user);
    }
}
