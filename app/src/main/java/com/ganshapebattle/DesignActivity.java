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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

import android.provider.MediaStore;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View.OnClickListener;
//import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.MLKit;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.models.Player;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.PlayerService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.utils.ImageUtils;
import com.ganshapebattle.view.DrawingView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.type.PublicPreviewAPI;

public class DesignActivity extends AppCompatActivity implements OnClickListener {

    private DrawingView drawView;
    private ImageButton drawBtn, eraseBtn, newBtn, saveBtn;
    private ImageButton currPaint;

    private float smallBrush, mediumBrush, largeBrush;
    private ImageView ganImage;
    private ImageView playerganimage;
    private Button transformBtn;
    private Context context;
    private Executor mainExecutor; // <-- Thêm

    // --- Biến cho Timer và Logic Game ---
    private LobbyService lobbyService;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private String username;
    private String lobbyId;
    private long designEndTimeMillis;
    private TextView textViewTimer;
    private boolean isActivityRunning = false;
    private static final long TIMER_INTERVAL = 1000;

    // --- NÂNG CẤP: Biến để lưu ảnh khi hết giờ ---
    private PlayerService playerService;
    private PictureService pictureService;
    private Player currentPlayer;
    private Picture currentPlayerPicture;
    private boolean isSavingImage = false;

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
        mainExecutor = ContextCompat.getMainExecutor(this); // <-- Khởi tạo

        // Lấy data từ Intent
        username = getIntent().getStringExtra("username");
        lobbyId = getIntent().getStringExtra("lobbyid");
        String beginVoteDateString = getIntent().getStringExtra("votetime");

        if (lobbyId == null || username == null) {
//            Toast.makeText(this, "Lỗi: Thiếu thông tin Lobby hoặc User.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        playerganimage = null;

        // Khởi tạo Services
        lobbyService = new LobbyService();
        timerHandler = new Handler(Looper.getMainLooper());
        playerService = new PlayerService();
        pictureService = new PictureService();

        textViewTimer = findViewById(R.id.textViewTimer);

        // (Code bind views từ lần trước)
        drawView = (DrawingView)findViewById(R.id.drawing);
        LinearLayout paintLayout = (LinearLayout)findViewById(R.id.paint_colors);
        try {
            LinearLayout firstColorWrapper = (LinearLayout) paintLayout.getChildAt(0);
            CardView firstCard = (CardView) firstColorWrapper.getChildAt(0);
            currPaint = (ImageButton) firstCard.getChildAt(0);
            currPaint.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.paint_pressed));
        } catch (Exception e) {
//            Toast.makeText(context, "Lỗi layout màu sắc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        try {
            ganImage = (ImageView)findViewById(R.id.ganimage);
            ganImage.setImageBitmap(drawView.getGanImage());
            ganImage.setOnClickListener(this);
            transformBtn = (Button)findViewById(R.id.transform_btn);
            transformBtn.setOnClickListener(this);
        } catch (ClassCastException e) {
//            Toast.makeText(context, "Lỗi XML: ganimage hoặc transform_btn sai kiểu.", Toast.LENGTH_LONG).show();
        }

        loadPlayerData();
        parseTimeAndStartLoop(beginVoteDateString);
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityRunning = false;
        stopTimerLoop();
    }

    // ... (Giữ nguyên các hàm: paintClicked, onClick) ...
    public void paintClicked(View view){
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
//                    Toast.makeText(getApplicationContext(), R.string.drawing_saved_to_gallery, Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(getApplicationContext(), R.string.oops_image_could_not_be_saved, Toast.LENGTH_SHORT).show();
                }
                drawView.destroyDrawingCache();
            });
            saveDialog.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
            saveDialog.show();
        } else if (viewId == R.id.ganimage) {
            if (playerganimage == null) return;
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

    /**
     * Hàm này được gọi bởi NÚT BẤM (có dialog)
     */
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
        // Executor mainExecutor = ContextCompat.getMainExecutor(this); // Đã chuyển lên onCreate

        Log.d("DesignActivity", "Calling static editImage function...");
        ListenableFuture<Bitmap> bitmapFuture = MLKit.editImage(originalBitmap, imageEditPrompt, mainExecutor);
        Futures.addCallback(bitmapFuture, new FutureCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap generatedBitmap) {
                drawView.setGanImage(generatedBitmap);
                ganImage.setImageBitmap(generatedBitmap);
                playerganimage = ganImage;
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


    // ==================================================================
    // --- NÂNG CẤP: LOGIC TẢI PLAYER VÀ LƯU ẢNH ---
    // ==================================================================

    /**
     * Tải Player và Picture của user này (gọi 1 lần lúc vào)
     */
    private void loadPlayerData() {
        if (username == null || lobbyId == null) return;
        playerService.getPlayerByIds(username, lobbyId, new SupabaseCallback<Player>() {
            @Override
            public void onSuccess(Player player) {
                if (player == null) {
//                    Toast.makeText(context, "Lỗi: Không tìm thấy thông tin Player", Toast.LENGTH_LONG).show();
                    return;
                }
                currentPlayer = player;
                Log.d("DesignActivity", "Tải Player thành công: " + player.getUsername());

                player.getPicture(new SupabaseCallback<Picture>() {
                    @Override
                    public void onSuccess(Picture picture) {
                        if (picture == null) {
//                            Toast.makeText(context, "Lỗi: Không tìm thấy Picture của Player", Toast.LENGTH_LONG).show();
                            return;
                        }
                        currentPlayerPicture = picture;
                        Log.d("DesignActivity", "Tải Picture thành công: " + picture.getId());
                    }
                    @Override
                    public void onFailure(Exception e) {
//                        Toast.makeText(context, "Lỗi tải Picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
//                Toast.makeText(context, "Lỗi tải Player: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * NÂNG CẤP: (Hàm cũ) Router để kiểm tra và quyết định
     */
    private void saveFinalImageAndProceed() {
        if (!isActivityRunning || isSavingImage) return;
        isSavingImage = true;

//        Toast.makeText(context, "Hết giờ! Đang nộp bài...", Toast.LENGTH_LONG).show();

        if (currentPlayer == null || currentPlayerPicture == null) {
//            Toast.makeText(context, "Lỗi: Không thể nộp bài (thiếu Player/Picture)", Toast.LENGTH_LONG).show();
            proceedToVotePhase(null);
            return;
        }

        // Bước 1: Kiểm tra xem đã có ảnh GAN chưa
        Bitmap currentGanImage = drawView.getGanImage();

        if (playerganimage == null) {
            // --- NÂNG CẤP: "CƠ HỘI CUỐI" ---
            Log.d("DesignActivity", "Hết giờ: Không có ảnh GAN. Bắt đầu 'Cơ hội cuối'...");
//            Toast.makeText(context, "Cơ hội cuối! Đang tạo ảnh AI...", Toast.LENGTH_SHORT).show();
            runSecondChanceTransformAndSave(); // Chạy ML
        } else {
            // Đã có ảnh GAN, lưu ảnh này
            Log.d("DesignActivity", "Hết giờ: Đã có ảnh GAN. Tiến hành lưu...");
            saveBitmapToSupabase(currentGanImage);
        }
    }

    /**
     * NÂNG CẤP: (Hàm mới) Chạy ML khi hết giờ
     */
    @OptIn(markerClass = PublicPreviewAPI.class)
    private void runSecondChanceTransformAndSave() {
        // Lấy ảnh vẽ tay làm input
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
        Bitmap originalBitmap = null;
        if (drawView.getDrawingCache() != null) {
            originalBitmap = Bitmap.createBitmap(drawView.getDrawingCache());
        }
        drawView.setDrawingCacheEnabled(false);

        if (originalBitmap == null) {
            Log.e("DesignActivity", "Cơ hội cuối thất bại: Không lấy được ảnh vẽ tay.");
            saveBitmapToSupabase(null); // Vẫn tiếp tục (với ảnh rỗng)
            return;
        }

        // (Sao chép logic từ startTransformEdit)
        String[] promptLines = getResources().getStringArray(R.array.prompt_photorealistic_segmentation_v2);
        StringBuilder promptBuilder = new StringBuilder();
        for (String line : promptLines) {
            promptBuilder.append(line).append("\n");
        }
        String imageEditPrompt = promptBuilder.toString();

        Log.d("DesignActivity", "Cơ hội cuối: Đang gọi MLKit.editImage...");

        final Bitmap finalOriginalBitmap = originalBitmap; // Biến final cho callback

        ListenableFuture<Bitmap> bitmapFuture = MLKit.editImage(originalBitmap, imageEditPrompt, mainExecutor);

        Futures.addCallback(bitmapFuture, new FutureCallback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap generatedBitmap) {
                Log.d("DesignActivity", "Cơ hội cuối THÀNH CÔNG. Đang lưu ảnh AI...");
                drawView.setGanImage(generatedBitmap); // Cập nhật view
                ganImage.setImageBitmap(generatedBitmap);
                saveBitmapToSupabase(generatedBitmap); // Lưu ảnh AI
                transformingDialog.cancel();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("DesignActivity", "Cơ hội cuối THẤT BẠI: " + t.getMessage());
//                Toast.makeText(context, "Tạo ảnh AI thất bại. Nộp ảnh vẽ tay...", Toast.LENGTH_SHORT).show();
                saveBitmapToSupabase(finalOriginalBitmap); // Lưu ảnh vẽ tay (fallback)
                transformingDialog.cancel();
            }
        }, mainExecutor);
    }


    /**
     * NÂNG CẤP: (Hàm mới) Logic lưu ảnh cuối cùng
     */
    private void saveBitmapToSupabase(Bitmap finalBitmap) {
        if (finalBitmap == null) {
//            Toast.makeText(context, "Lỗi: Không có ảnh cuối cùng để nộp!", Toast.LENGTH_LONG).show();
            proceedToVotePhase(null); // Vẫn tiếp tục dù lỗi
            return;
        }

        // Chuyển ảnh sang Base64
        String base64Image = ImageUtils.bitmapToBase64(finalBitmap, Bitmap.CompressFormat.PNG, 80); // Nén 80%

        if (base64Image == null) {
//            Toast.makeText(context, "Lỗi: Không thể chuyển đổi ảnh sang Base64!", Toast.LENGTH_LONG).show();
            proceedToVotePhase(null);
            return;
        }

        // Cập nhật và lưu Picture
        currentPlayerPicture.setImage(base64Image);

        pictureService.updatePicture(currentPlayerPicture.getId(), currentPlayerPicture, new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d("DesignActivity", "Nộp bài (cập nhật Picture) thành công!");
                proceedToVotePhase(null); // Chuyển sang giai đoạn Vote
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DesignActivity", "Lỗi nộp bài (update Picture): " + e.getMessage());
//                Toast.makeText(context, "Lỗi nộp bài: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                proceedToVotePhase(null); // Vẫn tiếp tục dù lỗi
            }
        });
    }

    // ==================================================================
    // --- LOGIC TIMER VÀ CHUYỂN ACTIVITY (ĐÃ SỬA) ---
    // ==================================================================

    // ... (Giữ nguyên các hàm: parseTimeAndStartLoop, startTimerLoop, stopTimerLoop, updateTimerDisplay) ...

    private void parseTimeAndStartLoop(String beginVoteDateString) {
        if (beginVoteDateString == null || beginVoteDateString.isEmpty()) {
//            Toast.makeText(context, "Lỗi: Không nhận được 'votetime' từ Intent.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date beginVoteDate = sdf.parse(beginVoteDateString);
            designEndTimeMillis = beginVoteDate.getTime();
            if (designEndTimeMillis <= System.currentTimeMillis()) {
//                Toast.makeText(context, "Lỗi: Thời gian kết thúc đã ở trong quá khứ.", Toast.LENGTH_LONG).show();
                handleTimeUp();
            } else {
                startTimerLoop();
            }
        } catch (ParseException e) {
            e.printStackTrace();
//            Toast.makeText(context, "Lỗi định dạng thời gian: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void startTimerLoop() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isActivityRunning) return;
                long currentTimeMillis = System.currentTimeMillis();
                long remainingMillis = designEndTimeMillis - currentTimeMillis;
                if (remainingMillis <= 0) {
                    stopTimerLoop();
                    if (textViewTimer != null) {
                        textViewTimer.setText("00:00");
                    }
                    handleTimeUp();
                } else {
                    updateTimerDisplay(remainingMillis);
                    timerHandler.postDelayed(this, TIMER_INTERVAL);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimerLoop() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateTimerDisplay(long remainingMillis) {
        if (textViewTimer == null) return;
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60));
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        textViewTimer.setText(timeString);
    }

    /**
     * NÂNG CẤP: Hàm này chỉ gọi hàm lưu ảnh
     */
    private void handleTimeUp() {
        // Chỉ gọi hàm lưu ảnh (phiên bản router)
        saveFinalImageAndProceed();
    }

    /**
     * NÂNG CẤP: Logic cũ (kiểm tra status)
     */
    private void proceedToVotePhase(Lobby lobbyFromSave) {
        if (!isActivityRunning) return;

        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby latestLobby) {
                if (latestLobby == null) {
//                    Toast.makeText(context, "Phòng không còn tồn tại.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                String status = latestLobby.getStatus();

                // === SỬA LOGIC CHUYỂN HƯỚNG ===
                if ("isPlaying".equals(status)) {
                    // Hết giờ vẽ -> chuyển sang voting (Chỉ Admin mới làm)
                    if (username.equals(latestLobby.getAdminUsername())) {
                        latestLobby.setStatus("isVoting");
                        lobbyService.updateLobby(lobbyId, latestLobby, new SupabaseCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                // SỬA: Chuyển sang LobbyRateVoteActivity
                                navigateToActivity(LobbyRateVoteActivity.class, latestLobby);
                            }
                            @Override
                            public void onFailure(Exception e) {
//                                Toast.makeText(context, "Lỗi cập nhật status, vẫn thử chuyển...", Toast.LENGTH_SHORT).show();
                                // SỬA: Chuyển sang LobbyRateVoteActivity
                                navigateToActivity(LobbyRateVoteActivity.class, latestLobby);
                            }
                        });
                    } else {
                        // Người chơi bình thường cũng chuyển sang LobbyRateVoteActivity
                        navigateToActivity(LobbyRateVoteActivity.class, latestLobby);
                    }

                } else if ("isVoting".equals(status)) {
                    // Nếu status đã là "isVoting"
                    // SỬA: Chuyển sang LobbyRateVoteActivity
                    navigateToActivity(LobbyRateVoteActivity.class, latestLobby);

                } else if ("isOver".equals(status) || "isEnd".equals(status)) { // Thêm kiểm tra "isEnd"
                    // Nếu game đã kết thúc
                    navigateToActivity(GameEndActivity.class, latestLobby);
                }
                // === KẾT THÚC SỬA LOGIC ===
            }
            @Override
            public void onFailure(Exception e) {
//                Toast.makeText(context, "Lỗi kiểm tra trạng thái hết giờ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // SỬA: Chuyển sang LobbyRateVoteActivity
                navigateToActivity(LobbyRateVoteActivity.class, null);
            }
        });
    }

    /**
     * Hàm helper để chuyển Activity (ĐÃ SỬA)
     */
    private void navigateToActivity(Class<?> activityClass, Lobby lobbyToPass) {
        if (!isActivityRunning) return;
        isActivityRunning = false;
        stopTimerLoop();

        Intent intent = new Intent(DesignActivity.this, activityClass);
        intent.putExtra("username", username);
        intent.putExtra("lobbyid", lobbyId); // LobbyRateVoteActivity cần "lobbyid"

        // Đảm bảo GameEndActivity nhận đúng key "lobby_id"
        if (activityClass == GameEndActivity.class) {
            intent.putExtra("lobby_id", lobbyId);
        }

        startActivity(intent);
        finish();
    }
}