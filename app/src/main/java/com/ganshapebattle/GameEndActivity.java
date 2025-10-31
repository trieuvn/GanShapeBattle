package com.ganshapebattle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

public class GameEndActivity extends AppCompatActivity {

    private static final String TAG = "GameEndActivity";

    private VideoView backgroundVideo;
    private String lobbyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_end);

        lobbyId = getIntent().getStringExtra("lobby_id");
        if (lobbyId == null) {
            Log.e(TAG, "No lobby ID provided"); finish();
            return;
        }

        // Khởi tạo VideoView
        backgroundVideo = findViewById(R.id.background_video);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.endgame);
        backgroundVideo.setVideoURI(videoUri);
        backgroundVideo.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            backgroundVideo.start();
        });
        backgroundVideo.setOnCompletionListener(mp -> startLeaderboard());
        Log.d(TAG, "GameEndActivity started with lobbyId: " + lobbyId);
    }
    private void startLeaderboard() {
        Intent intent = new Intent(this, Leaderboard.class);
        intent.putExtra("lobbyid", lobbyId);
        startActivity(intent);
        finish();
    }
}
