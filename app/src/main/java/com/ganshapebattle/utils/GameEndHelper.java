package com.ganshapebattle.utils;

import android.content.Context;
import android.content.Intent;

import com.ganshapebattle.GameEndActivity;

/**
 * Helper class để gọi GameEndActivity
 * Sử dụng class này để dễ dàng mở màn hình kết thúc game
 */
public class GameEndHelper {
    
    /**
     * Mở GameEndActivity với lobby ID
     * @param context Context hiện tại
     * @param lobbyId ID của lobby cần hiển thị kết quả
     */
    public static void openGameEnd(Context context, String lobbyId) {
        Intent intent = new Intent(context, GameEndActivity.class);
        intent.putExtra("lobby_id", lobbyId);
        context.startActivity(intent);
    }
    
    /**
     * Mở GameEndActivity với lobby ID và tự động finish activity hiện tại
     * @param context Context hiện tại
     * @param lobbyId ID của lobby cần hiển thị kết quả
     */
    public static void openGameEndAndFinish(Context context, String lobbyId) {
        Intent intent = new Intent(context, GameEndActivity.class);
        intent.putExtra("lobby_id", lobbyId);
        context.startActivity(intent);
        
        // Finish current activity if it's an Activity
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).finish();
        }
    }
}


