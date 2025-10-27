package com.ganshapebattle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ganshapebattle.adapters.PlayerScoreAdapter;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GameEndActivity extends AppCompatActivity {
    
    private static final String TAG = "GameEndActivity";
    private static final int COUNTDOWN_DURATION = 10000; // 10 seconds
    
    // UI Components
    private LinearLayout loadingContainer;
    private ScrollView scoreboardContainer;
    private ProgressBar loadingSpinner;
    private TextView loadingText;
    private TextView countdownTimer;
    private TextView gameOverTitle;
    private TextView winnerAnnouncement;
    private TextView winnerName;
    private RecyclerView playersRecyclerView;
    private Button backToMenuBtn;
    
    // Services
    private PlayerService playerService;
    private LobbyService lobbyService;
    
    // Data
    private String lobbyId;
    private List<Player> players;
    private PlayerScoreAdapter playerScoreAdapter;
    private CountDownTimer countDownTimer;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_end);
        
        // Initialize services
        playerService = new PlayerService();
        lobbyService = new LobbyService();
        mainHandler = new Handler(getMainLooper());
        
        // Get lobby ID from intent
        lobbyId = getIntent().getStringExtra("lobby_id");
        if (lobbyId == null) {
            Log.e(TAG, "No lobby ID provided");
            finish();
            return;
        }
        
        // Initialize UI
        initializeViews();
        setupRecyclerView();
        
        // Start the end game process
        endGame();
    }
    
    private void initializeViews() {
        loadingContainer = findViewById(R.id.loading_container);
        scoreboardContainer = findViewById(R.id.scoreboard_container);
        loadingSpinner = findViewById(R.id.loading_spinner);
        loadingText = findViewById(R.id.loading_text);
        countdownTimer = findViewById(R.id.countdown_timer);
        gameOverTitle = findViewById(R.id.game_over_title);
        winnerAnnouncement = findViewById(R.id.winner_announcement);
        winnerName = findViewById(R.id.winner_name);
        playersRecyclerView = findViewById(R.id.players_recycler_view);
        backToMenuBtn = findViewById(R.id.back_to_menu_btn);
        
        // Setup back to menu button
        backToMenuBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
    
    private void setupRecyclerView() {
        players = new ArrayList<>();
        playerScoreAdapter = new PlayerScoreAdapter(players);
        playersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        playersRecyclerView.setAdapter(playerScoreAdapter);
    }
    
    /**
     * Main function to handle game end process
     * Shows loading animation -> fetches data -> shows scoreboard -> auto return to menu
     */
    public void endGame() {
        Log.d(TAG, "Starting end game process for lobby: " + lobbyId);
        
        // Start loading animation
        startLoadingAnimation();
        
        // Start countdown timer
        startCountdownTimer();
        
        // Fetch players data
        fetchPlayersData();
    }
    
    private void startLoadingAnimation() {
        // Animate loading spinner
        ObjectAnimator rotation = ObjectAnimator.ofFloat(loadingSpinner, "rotation", 0f, 360f);
        rotation.setDuration(1000);
        rotation.setRepeatCount(ObjectAnimator.INFINITE);
        rotation.start();
        
        // Animate loading text with fade in/out
        startTextAnimation();
    }
    
    private void startTextAnimation() {
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(1000);
        fadeOut.setStartOffset(1000);
        
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(fadeIn);
        animationSet.addAnimation(fadeOut);
        animationSet.setRepeatCount(Animation.INFINITE);
        
        loadingText.startAnimation(animationSet);
    }
    
    private void startCountdownTimer() {
        countDownTimer = new CountDownTimer(COUNTDOWN_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                countdownTimer.setText(String.valueOf(secondsRemaining));
                
                // Animate countdown number
                animateCountdownNumber();
            }
            
            @Override
            public void onFinish() {
                countdownTimer.setText("0");
                // Transition to scoreboard
                transitionToScoreboard();
            }
        };
        countDownTimer.start();
    }
    
    private void animateCountdownNumber() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(200);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            
            @Override
            public void onAnimationEnd(Animation animation) {
                // Scale back to normal
                ScaleAnimation scaleBack = new ScaleAnimation(
                        1.2f, 1.0f, 1.2f, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                );
                scaleBack.setDuration(200);
                countdownTimer.startAnimation(scaleBack);
            }
            
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        countdownTimer.startAnimation(scaleAnimation);
    }
    
    private void fetchPlayersData() {
        // Get all players in the lobby
        playerService.getAllPlayers(new SupabaseCallback<List<Player>>() {
            @Override
            public void onSuccess(List<Player> allPlayers) {
                // Filter players for this lobby
                List<Player> lobbyPlayers = new ArrayList<>();
                for (Player player : allPlayers) {
                    if (lobbyId.equals(player.getLobbyId())) {
                        lobbyPlayers.add(player);
                    }
                }
                
                // Sort players by score (descending)
                Collections.sort(lobbyPlayers, new Comparator<Player>() {
                    @Override
                    public int compare(Player p1, Player p2) {
                        return Integer.compare(p2.getPoint(), p1.getPoint());
                    }
                });
                
                players.clear();
                players.addAll(lobbyPlayers);
                
                Log.d(TAG, "Fetched " + players.size() + " players for lobby " + lobbyId);
                
                // Update UI on main thread
                mainHandler.post(() -> {
                    playerScoreAdapter.notifyDataSetChanged();
                    if (!players.isEmpty()) {
                        winnerName.setText(players.get(0).getUsername());
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch players data", e);
                // Show error message
                mainHandler.post(() -> {
                    loadingText.setText("Lỗi tải dữ liệu...");
                });
            }
        });
    }
    
    private void transitionToScoreboard() {
        // Hide loading container with slide up animation
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(loadingContainer, "translationY", 0f, -loadingContainer.getHeight());
        slideUp.setDuration(500);
        slideUp.setInterpolator(new AccelerateDecelerateInterpolator());
        slideUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loadingContainer.setVisibility(View.GONE);
            }
        });
        
        // Show scoreboard container with slide up animation
        scoreboardContainer.setVisibility(View.VISIBLE);
        scoreboardContainer.setTranslationY(scoreboardContainer.getHeight());
        ObjectAnimator slideUpScoreboard = ObjectAnimator.ofFloat(scoreboardContainer, "translationY", scoreboardContainer.getHeight(), 0f);
        slideUpScoreboard.setDuration(500);
        slideUpScoreboard.setInterpolator(new AccelerateDecelerateInterpolator());
        
        // Start animations
        slideUp.start();
        slideUpScoreboard.start();
        
        // Show back to menu button after a delay
        new Handler().postDelayed(() -> {
            backToMenuBtn.setVisibility(View.VISIBLE);
            backToMenuBtn.setAlpha(0f);
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(backToMenuBtn, "alpha", 0f, 1f);
            fadeIn.setDuration(300);
            fadeIn.start();
        }, 1000);
        
        // Auto return to menu after 5 seconds
        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 5000);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
