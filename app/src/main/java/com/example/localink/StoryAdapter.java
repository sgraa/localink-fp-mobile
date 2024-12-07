// File: StoryAdapter.java
package com.example.localink;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private List<Media> mediaList;
    private Context context;

    public StoryAdapter(List<Media> mediaList, Context context) {
        this.mediaList = mediaList;
        this.context = context;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate item_story.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Media media = mediaList.get(position);
        String url = media.getDownloadUrl();

        if (media.getType().equals("video")) {
            holder.storyImageView.setVisibility(View.GONE);
            holder.storyVideoView.setVisibility(View.VISIBLE);

            holder.storyVideoView.setVideoURI(Uri.parse(url));
            MediaController mediaController = new MediaController(context);
            holder.storyVideoView.setMediaController(mediaController);
            mediaController.setAnchorView(holder.storyVideoView);
            holder.storyVideoView.start();
        } else {
            holder.storyVideoView.setVisibility(View.GONE);
            holder.storyImageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(url)
                    .into(holder.storyImageView);
        }
    }

    @Override
    public int getItemCount() {
        return mediaList.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView storyImageView;
        VideoView storyVideoView;

        public StoryViewHolder(View itemView) {
            super(itemView);
            storyImageView = itemView.findViewById(R.id.storyImageView);
            storyVideoView = itemView.findViewById(R.id.storyVideoView);
        }
    }
}
