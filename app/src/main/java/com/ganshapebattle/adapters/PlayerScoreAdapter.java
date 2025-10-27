package com.ganshapebattle.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Player;

import java.util.List;

public class PlayerScoreAdapter extends RecyclerView.Adapter<PlayerScoreAdapter.PlayerScoreViewHolder> {
    
    private List<Player> players;
    
    public PlayerScoreAdapter(List<Player> players) {
        this.players = players;
    }
    
    @NonNull
    @Override
    public PlayerScoreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_score, parent, false);
        return new PlayerScoreViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PlayerScoreViewHolder holder, int position) {
        Player player = players.get(position);
        
        // Set rank (position + 1)
        holder.playerRank.setText(String.valueOf(position + 1));
        
        // Set player name
        holder.playerName.setText(player.getUsername());
        
        // Set player score
        holder.playerScore.setText("Score: " + player.getPoint());
        
        // Show trophy for winner (first place)
        if (position == 0) {
            holder.trophyIcon.setVisibility(View.VISIBLE);
        } else {
            holder.trophyIcon.setVisibility(View.GONE);
        }
        
        // Set different background for top 3 players
        if (position < 3) {
            holder.itemView.setBackgroundResource(R.drawable.top_player_background);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.player_item_background);
        }
    }
    
    @Override
    public int getItemCount() {
        return players.size();
    }
    
    public static class PlayerScoreViewHolder extends RecyclerView.ViewHolder {
        TextView playerRank;
        ImageView playerAvatar;
        TextView playerName;
        TextView playerScore;
        ImageView trophyIcon;
        
        public PlayerScoreViewHolder(@NonNull View itemView) {
            super(itemView);
            playerRank = itemView.findViewById(R.id.player_rank);
            playerAvatar = itemView.findViewById(R.id.player_avatar);
            playerName = itemView.findViewById(R.id.player_name);
            playerScore = itemView.findViewById(R.id.player_score);
            trophyIcon = itemView.findViewById(R.id.trophy_icon);
        }
    }
}


