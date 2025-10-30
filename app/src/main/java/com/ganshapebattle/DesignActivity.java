package com.ganshapebattle;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException; // <-- Sửa API
import java.text.SimpleDateFormat; // <-- Sửa API
import java.util.Calendar; // <-- Sửa API
import java.util.Date; // <-- Sửa API
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.MLKit;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.view.DrawingView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.PublicPreviewAPI;

public class DesignActivity extends AppCompatActivity implements OnClickListener {

    private DrawingView drawView;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn;

    // --- SỬA LỖI 1: ClassCastException (currPaint phải là ImageButton) ---
    private ImageButton currPaint;

    private float smallBrush, mediumBrush, largeBrush;
    private ImageView ganImage;
    private Button transformBtn;
    private Context context;

    // --- Biến cho Timer và Logic Game ---
    private LobbyService lobbyService;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private String username;
    private String lobbyId;
    private long designEndTimeMillis; // Thời điểm kết thúc (để so sánh)
    private TextView textViewTimer;
    private boolean isActivityRunning = false;
    private static final long TIMER_INTERVAL = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_design);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        context = this;
        isActivityRunning = true;

        // --- Lấy data từ Intent (LOGIC ĐÚNG) ---
        username = getIntent().getStringExtra("username");
        lobbyId = getIntent().getStringExtra("lobbyid");

        // Đây là chuỗi "yyyy-MM-dd HH:mm:ss" được gửi từ GameLoadActivity
        String beginVoteDateString = getIntent().getStringExtra("votetime");

        if (lobbyId == null || username == null) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin Lobby hoặc User.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Khởi tạo Services và UI ---
        lobbyService = new LobbyService();
        timerHandler = new Handler(Looper.getMainLooper());

        // !!! CẢNH BÁO: Đảm bảo bạn đã thêm R.id.textViewTimer vào activity_design.xml
        textViewTimer = findViewById(R.id.textViewTimer);
        if (textViewTimer == null) {
            Toast.makeText(this, "Lỗi: Thiếu R.id.textViewTimer trong XML", Toast.LENGTH_LONG).show();
        }

        drawView = (DrawingView)findViewById(R.id.drawing);
        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);

        // --- SỬA LỖI 2: Logic lấy currPaint ---
        try {
            LinearLayout firstColorWrapper = (LinearLayout) paintLayout.getChildAt(0);
            CardView firstCard = (CardView) firstColorWrapper.getChildAt(0);
            currPaint = (ImageButton) firstCard.getChildAt(0);
            currPaint.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.paint_pressed));
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi layout màu sắc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);
        drawView.setBrushSize(mediumBrush);

        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        // Giả sử R.id.ganimage là ImageView, R.id.transform_btn là Button
        // Nếu không đúng, bạn sẽ lại gặp ClassCastException
        try {
            ganImage = (ImageView)findViewById(R.id.ganimage);
            ganImage.setImageBitmap(drawView.getGanImage());
            ganImage.setOnClickListener(this);

            transformBtn = (Button)findViewById(R.id.transform_btn);
            transformBtn.setOnClickListener(this);
        } catch (ClassCastException e) {
            Toast.makeText(context, "Lỗi XML: ganimage hoặc transform_btn sai kiểu.", Toast.LENGTH_LONG).show();
        }


        // --- Bắt đầu logic timer (LOGIC ĐÚNG) ---
        parseTimeAndStartLoop(beginVoteDateString);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityRunning = false;
        stopTimerLoop();
    }

    public void paintClicked(View view){
        // --- SỬA LỖI 3: Logic này giờ đã đúng (vì currPaint là ImageButton) ---
        if(view!=currPaint && currPaint != null){
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
        }
        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());
    }

    // ... (Hàm startTransformEdit và onClick giữ nguyên, không cần sửa) ...
    // ... (Giữ nguyên code startTransformEdit) ...
    // ... (Giữ nguyên code onClick) ...
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void startTransformEdit() {
        // ... (Giữ nguyên code của bạn)
        AlertDialog.Builder transformingDialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);
        transformingDialogBuilder.setView(dialogView);
        ImageView gifImageView = dialogView.findViewById(R.id.gif_image_view);
        Glide.with(context).load(R.drawable.transforming).into(gifImageView);
        transformingDialogBuilder.setTitle(getResources().getString(R.string.transforming));
        AlertDialog transformingDialog = transformingDialogBuilder.create();
        transformingDialog.show();
        drawView.setDrawingCacheEnabled(true);
        Bitmap originalBitmap = drawView.getDrawingCache();
        String[] promptLines = getResources().getStringArray(R.array.prompt_photorealistic_segmentation_v2);
        StringBuilder promptBuilder = new StringBuilder();
        for (String line : promptLines) {
            promptBuilder.append(line).append("\n");
        }
        String imageEditPrompt = promptBuilder.toString();
        Executor mainExecutor = ContextCompat.getMainExecutor(this);
        Log.d("DesignActivity", "Calling static editImage function...");
        ListenableFuture<Bitmap> bitmapFuture = MLKit.editImage(originalBitmap, imageEditPrompt, mainExecutor);
        Futures.addCallback(bitmapFuture, new FutureCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap generatedBitmap) {
                drawView.setGanImage(generatedBitmap);
                ganImage.setImageBitmap(generatedBitmap);
                transformingDialog.cancel();
                AlertDialog.Builder ganImageDialog = new AlertDialog.Builder(context);
                ganImageDialog.setTitle(R.string.super_gan_image);
                ImageView imageView = new ImageView(context);
                imageView.setImageBitmap(drawView.getGanImage());
                int padding = 60;
                imageView.setPadding(padding, padding, padding, padding);
                imageView.setAdjustViewBounds(true);
                ganImageDialog.setView(imageView);
                ganImageDialog.setPositiveButton(R.string.ok, null);
                ganImageDialog.create().show();
            }
            @Override
            public void onFailure(Throwable t) {
                AlertDialog.Builder saveDialog = new AlertDialog.Builder(context);
                saveDialog.setTitle("FAIL");
                saveDialog.setMessage("That sucks...");
                saveDialog.show();
                transformingDialog.cancel();
            }
        }, mainExecutor);
        drawView.destroyDrawingCache();
    }

    @Override
    public void onClick(View view){
        // ... (Giữ nguyên code của bạn)
        int viewId = view.getId();

        if(viewId == R.id.draw_btn){
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(v -> {
                drawView.setBrushSize(smallBrush);
                drawView.setLastBrushSize(smallBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(v -> {
                drawView.setBrushSize(mediumBrush);
                drawView.setLastBrushSize(mediumBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(v -> {
                drawView.setBrushSize(largeBrush);
                drawView.setLastBrushSize(largeBrush);
                drawView.setErase(false);
                brushDialog.dismiss();
            });
            brushDialog.show();
        } else if(viewId == R.id.erase_btn){
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(v -> {
                drawView.setErase(true);
                drawView.setBrushSize(smallBrush);
                brushDialog.dismiss();
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(v -> {
                drawView.setErase(true);
                drawView.setBrushSize(mediumBrush);
                brushDialog.dismiss();
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(v -> {
                drawView.setErase(true);
                drawView.setBrushSize(largeBrush);
                brushDialog.dismiss();
            });
            brushDialog.show();
        } else if(viewId == R.id.new_btn){
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle(R.string.new_drawing);
            newDialog.setMessage(R.string.start_new_msg);
            newDialog.setPositiveButton(R.string.yes, (dialog, which) -> {
                drawView.startNew();
                dialog.dismiss();
            });
            newDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            newDialog.show();
        } else if(viewId == R.id.save_btn){
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle(R.string.savedrawing);
            saveDialog.setMessage(R.string.savedrawing_msg);
            saveDialog.setPositiveButton(R.string.yes, (dialog, which) -> {
                drawView.setDrawingCacheEnabled(true);
                String imgSaved = MediaStore.Images.Media.insertImage(
                        getContentResolver(), drawView.getDrawingCache(),
                        UUID.randomUUID().toString() + ".png", "drawing");
                if (imgSaved != null) {
                    Toast.makeText(getApplicationContext(), R.string.drawing_saved_to_gallery, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.oops_image_could_not_be_saved, Toast.LENGTH_SHORT).show();
                }
                drawView.destroyDrawingCache();
            });
            saveDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            saveDialog.show();
        } else if (viewId == R.id.ganimage) {
            AlertDialog.Builder ganImageDialog = new AlertDialog.Builder(this);
            ganImageDialog.setTitle(R.string.super_gan_image);
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(drawView.getGanImage());
            int padding = 60;
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setAdjustViewBounds(true);
            ganImageDialog.setView(imageView);
            ganImageDialog.setPositiveButton(R.string.ok, null);
            ganImageDialog.create().show();
        } else if (viewId == R.id.transform_btn) {
            startTransformEdit();
        }
    }


    // ==================================================================
    // --- PHẦN LOGIC TIMER (ĐÃ SỬA LỖI ĐỂ NHẬN ĐÚNG FORMAT) ---
    // ==================================================================

    /**
     * Bước 1: Parse chuỗi thời gian nhận được và bắt đầu vòng lặp
     */
    private void parseTimeAndStartLoop(String beginVoteDateString) {
        if (beginVoteDateString == null || beginVoteDateString.isEmpty()) {
            Toast.makeText(context, "Lỗi: Không nhận được 'votetime' từ Intent.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            // Format này PHẢI KHỚP với format trong GameLoadActivity
            // (yyyy-MM-dd HH:mm:ss)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date beginVoteDate = sdf.parse(beginVoteDateString);

            designEndTimeMillis = beginVoteDate.getTime();

            // Kiểm tra xem thời gian kết thúc có ở quá khứ không
            if (designEndTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(context, "Lỗi: Thời gian kết thúc đã ở trong quá khứ.", Toast.LENGTH_LONG).show();
                // Hết giờ ngay lập tức
                handleTimeUp();
            } else {
                // Bắt đầu vòng lặp
                startTimerLoop();
            }

        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(context, "Lỗi định dạng thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Bước 2: Bắt đầu vòng lặp (Handler)
     */
    private void startTimerLoop() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isActivityRunning) return;

                long currentTimeMillis = System.currentTimeMillis();
                long remainingMillis = designEndTimeMillis - currentTimeMillis;

                if (remainingMillis <= 0) {
                    // Hết giờ
                    stopTimerLoop();
                    if (textViewTimer != null) {
                        textViewTimer.setText("00:00");
                    }
                    handleTimeUp();
                } else {
                    // Vẫn còn giờ, cập nhật UI
                    updateTimerDisplay(remainingMillis);
                    timerHandler.postDelayed(this, TIMER_INTERVAL);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Dừng vòng lặp (khi hết giờ hoặc khi thoát Activity)
     */
    private void stopTimerLoop() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    /**
     * Cập nhật TextView đếm ngược
     */
    private void updateTimerDisplay(long remainingMillis) {
        if (textViewTimer == null) return;
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)); // Tổng số phút còn lại
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        textViewTimer.setText(timeString);
    }

    /**
     * Bước 3: Xử lý khi hết giờ
     */
    private void handleTimeUp() {
        if (!isActivityRunning) return;

        // Truy vấn lại lobby để lấy status MỚI NHẤT
        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby latestLobby) {
                if (latestLobby == null) {
                    Toast.makeText(context, "Phòng không còn tồn tại.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                String status = latestLobby.getStatus();

                if ("isPlaying".equals(status)) {
                    // Hết giờ vẽ -> chuyển sang voting
                    latestLobby.setStatus("isVoting");
                    lobbyService.updateLobby(lobbyId, latestLobby, new SupabaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            navigateToActivity(GameVoteActivity.class, latestLobby);
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(context, "Lỗi cập nhật status, vẫn thử chuyển...", Toast.LENGTH_SHORT).show();
                            navigateToActivity(GameVoteActivity.class, latestLobby);
                        }
                    });

                } else if ("isVoting".equals(status)) {
                    // Nếu status đã là "isVoting"
                    navigateToActivity(GameVoteActivity.class, latestLobby);

                } else if ("isOver".equals(status)) {
                    // Nếu game đã kết thúc
                    navigateToActivity(GameEndActivity.class, latestLobby);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "Lỗi kiểm tra trạng thái hết giờ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Thử chuyển sang Vote như một giải pháp dự phòng
                navigateToActivity(GameVoteActivity.class, null); // Gửi null
            }
        });
    }

    /**
     * Hàm helper để chuyển Activity và đóng Activity hiện tại
     * (Đã cập nhật để gửi votetime là INT)
     */
    private void navigateToActivity(Class<?> activityClass, Lobby lobbyToPass) {
        if (!isActivityRunning) return;
        isActivityRunning = false;
        stopTimerLoop();

        Intent intent = new Intent(DesignActivity.this, activityClass);
        intent.putExtra("username", username);
        intent.putExtra("lobbyid", lobbyId);

        // Gửi votetime (là INT - số phút) cho Activity tiếp theo
        if (activityClass == GameVoteActivity.class) {
            int voteTime = 5; // Mặc định 5 phút nếu lỗi
            String beginVoteDate = null;

            if (lobbyToPass != null) {
                voteTime = lobbyToPass.getVoteTime();
                // Lấy thời gian BẮT ĐẦU vote (chính là thời gian KẾT THÚC design)
                beginVoteDate = lobbyToPass.getBeginVoteDate();
            }

            intent.putExtra("votetime_duration", voteTime); // Gửi số phút
            intent.putExtra("votetime_start", beginVoteDate); // Gửi mốc thời gian bắt đầu
        }

        startActivity(intent);
        finish();
    }
}