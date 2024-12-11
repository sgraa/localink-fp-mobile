package com.example.localink;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.localink.Media;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserStoryAdapter extends RecyclerView.Adapter<UserStoryAdapter.UserStoryViewHolder> {

    private List<Media> mediaList;
    private Context context;
    private FirebaseFirestore firestore;

    public UserStoryAdapter(List<Media> mediaList, Context context) {
        this.mediaList = mediaList;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserStoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new UserStoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserStoryViewHolder holder, int position) {
        Media media = mediaList.get(position);

        // Load the image from the URL using Glide
        Glide.with(context)
                .load(media.getDownloadUrl())
                .into(holder.storyImageView);

        // Fetch the username of the friend from Firestore
        firestore.collection("users").document(media.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        holder.usernameTextView.setText(username);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class UserStoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImageView;
        TextView usernameTextView;

        public UserStoryViewHolder(View itemView) {
            super(itemView);
            storyImageView = itemView.findViewById(R.id.storyImageView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView); // Only username TextView
        }
    }
}
