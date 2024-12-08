package com.example.localink;

import android.content.Context;
import android.content.Intent;
import android.view.View;

public class Navbar {

    private Context context;

    public Navbar(Context context) {
        this.context = context;
    }

    public void setupNavigation(View navHome, View navFriends, View navStory) {
        // Klik Home
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(context instanceof MainActivity)) {
                    Intent intent = new Intent(context, MainActivity.class);
                    context.startActivity(intent);
                }
            }
        });

        // Klik Friends
        navFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(context instanceof FriendsActivity)) {
                    Intent intent = new Intent(context, FriendsActivity.class);
                    context.startActivity(intent);
                }
            }
        });

        // Klik Story
        navStory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(context instanceof StoryActivity)) {
                    Intent intent = new Intent(context, StoryActivity.class);
                    context.startActivity(intent);
                }
            }
        });
    }
}
