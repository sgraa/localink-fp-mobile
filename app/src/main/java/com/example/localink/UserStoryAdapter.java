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
import java.util.List;
import java.util.ArrayList;



public class UserStoryAdapter extends RecyclerView.Adapter<UserStoryAdapter.UserStoryViewHolder> {

    private List<Media> mediaList;
    private Context context;

    public UserStoryAdapter(List<Media> mediaList, Context context) {
        this.mediaList = mediaList;
        this.context = context;
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

        // Load the image from the URL using Glide or Picasso
        Glide.with(context)
                .load(media.getDownloadUrl())
                .into(holder.storyImageView);

        // Optional: Set other data like the time
        holder.storyTime.setText("Story created at: " + media.getCreatedAt());
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class UserStoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImageView;
        TextView storyTime;

        public UserStoryViewHolder(View itemView) {
            super(itemView);
            storyImageView = itemView.findViewById(R.id.storyImageView);
            storyTime = itemView.findViewById(R.id.storyTime);
        }
    }
}
