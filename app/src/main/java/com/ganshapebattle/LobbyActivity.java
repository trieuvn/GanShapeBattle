package com.ganshapebattle;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

// import io.supabase.client.SupabaseClient; // Import thư viện Supabase của bạn

public class LobbyActivity extends AppCompatActivity {

    // --- UI Elements ---
    private Button buttonCreateLobby, buttonBegin, buttonDelete;
    private ImageView imageViewQRCode;
    private RadioGroup radioGroupMode;
    private TextView textViewLobbyId;
    private LinearLayout layoutLobbyInfo, layoutControls;

    // --- Supabase & Data ---
    // private SupabaseClient supabase;
    private String currentLobbyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        // Khởi tạo Supabase client của bạn ở đây
        // supabase = SupabaseClientManager.getInstance();

        bindViews();
        setupListeners();
    }

    /**
     * Ánh xạ các thành phần từ layout XML vào biến Java.
     */
    private void bindViews() {
        buttonCreateLobby = findViewById(R.id.buttonCreateLobby);
        buttonBegin = findViewById(R.id.buttonBegin);
        buttonDelete = findViewById(R.id.buttonDelete);
        imageViewQRCode = findViewById(R.id.imageViewQRCode);
        radioGroupMode = findViewById(R.id.radioGroupMode);
        textViewLobbyId = findViewById(R.id.textViewLobbyId);
        layoutLobbyInfo = findViewById(R.id.layoutLobbyInfo);
        layoutControls = findViewById(R.id.layoutControls);
    }

    /**
     * Thiết lập các bộ lắng nghe sự kiện cho các nút.
     */
    private void setupListeners() {
        buttonCreateLobby.setOnClickListener(v -> createLobby());
        buttonBegin.setOnClickListener(v -> beginGame());
        buttonDelete.setOnClickListener(v -> deleteLobby());

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            String newMode = (checkedId == R.id.radioButtonVote) ? "vote" : "rate";
            setMode(newMode);
        });
    }

    /**
     * Bước 1-7: Tạo phòng, lưu lên Supabase và hiển thị mã QR.
     */
    private void createLobby() {
        try {
            // Mô phỏng tạo một object Lobby với giá trị mặc định
            HashMap<String, Object> newLobby = new HashMap<>();
            newLobby.put("mode", "vote"); // Mặc định là vote
            newLobby.put("status", "waiting"); // Trạng thái chờ
            newLobby.put("created_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date()));
            
            // --- GỌI SUPABASE ĐỂ INSERT ---
            // supabase.from("lobbies").insert(newLobby).execute(response -> {
            //     if (response.isSuccess()) {
            //         // Lấy ID phòng vừa tạo từ response
            //         currentLobbyId = response.getData().get("id").getAsString();
            //         runOnUiThread(() -> {
            //             Toast.makeText(this, "Tạo phòng thành công!", Toast.LENGTH_SHORT).show();
            //             generateAndShowQRCode(currentLobbyId);
            //             updateUIAfterCreation();
            //         });
            //     } else {
            //         runOnUiThread(() -> {
            //             Toast.makeText(this, "Lỗi tạo phòng: " + response.getError().getMessage(), Toast.LENGTH_LONG).show();
            //         });
            //     }
            // });
            
            // ---- GIẢ LẬP KHI KHÔNG CÓ SUPABASE ----
            currentLobbyId = UUID.randomUUID().toString().substring(0, 6); // Tạo ID giả
            Toast.makeText(this, "Tạo phòng thành công!", Toast.LENGTH_SHORT).show();
            generateAndShowQRCode(currentLobbyId);
            updateUIAfterCreation();
            // ---- KẾT THÚC GIẢ LẬP ----
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Bước 8-13: Thay đổi chế độ chơi và cập nhật lên Supabase.
     */
    private void setMode(String mode) {
        if (currentLobbyId == null) {
            Toast.makeText(this, "Chưa có phòng chơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("mode", mode);
            updates.put("updated_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date()));
            
            // --- GỌI SUPABASE ĐỂ UPDATE ---
            // supabase.from("lobbies").update(updates).eq("id", currentLobbyId).execute(response -> {
            //     if(response.isSuccess()) {
            //         runOnUiThread(() -> Toast.makeText(this, "Đã đổi chế độ: " + mode, Toast.LENGTH_SHORT).show());
            //     } else {
            //         runOnUiThread(() -> Toast.makeText(this, "Lỗi cập nhật chế độ: " + response.getError().getMessage(), Toast.LENGTH_LONG).show());
            //     }
            // });

            // ---- GIẢ LẬP ----
            Toast.makeText(this, "Đã đổi chế độ: " + mode, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi cập nhật chế độ: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Bước 14-20: Bắt đầu trò chơi.
     */
    private void beginGame() {
        if (currentLobbyId == null) {
            Toast.makeText(this, "Chưa có phòng chơi!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            String beginDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date());
            
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("status", "in_progress");
            updates.put("begin_date", beginDate);
            updates.put("updated_at", beginDate);

            // --- GỌI SUPABASE ĐỂ UPDATE ---
            // supabase.from("lobbies").update(updates).eq("id", currentLobbyId).execute(response -> {
            //     if(response.isSuccess()) {
            //         runOnUiThread(() -> {
            //             Toast.makeText(this, "Trò chơi bắt đầu!", Toast.LENGTH_SHORT).show();
            //             // Chuyển sang màn hình chơi game tại đây
            //             // Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
            //             // intent.putExtra("lobby_id", currentLobbyId);
            //             // startActivity(intent);
            //         });
            //     } else {
            //         runOnUiThread(() -> Toast.makeText(this, "Lỗi bắt đầu trò chơi: " + response.getError().getMessage(), Toast.LENGTH_LONG).show());
            //     }
            // });

            // ---- GIẢ LẬP ----
            Toast.makeText(this, "Trò chơi bắt đầu!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi bắt đầu trò chơi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Bước 21-28: Xóa phòng và người chơi liên quan khỏi Supabase.
     */
    private void deleteLobby() {
        if (currentLobbyId == null) {
            Toast.makeText(this, "Chưa có phòng chơi!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // --- GỌI SUPABASE ĐỂ DELETE ---
            // Theo sơ đồ, bạn cần xóa người chơi liên quan trước, sau đó mới xóa phòng.
            // Điều này có thể được xử lý tốt nhất bằng `CASCADE` trong database hoặc gọi 2 lệnh.
            // // 1. Xóa người chơi liên quan
            // supabase.from("players").delete().eq("lobby_id", currentLobbyId).execute(playerResponse -> {
            //     // 2. Xóa phòng
            //     supabase.from("lobbies").delete().eq("id", currentLobbyId).execute(lobbyResponse -> {
            //         if(lobbyResponse.isSuccess()) {
            //             runOnUiThread(() -> {
            //                 Toast.makeText(this, "Đã xóa phòng.", Toast.LENGTH_SHORT).show();
            //                 resetUI();
            //             });
            //         } else {
            //             runOnUiThread(() -> Toast.makeText(this, "Lỗi xóa phòng: " + lobbyResponse.getError().getMessage(), Toast.LENGTH_LONG).show());
            //         }
            //     });
            // });
            
            // ---- GIẢ LẬP ----
            Toast.makeText(this, "Đã xóa phòng.", Toast.LENGTH_SHORT).show();
            resetUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xóa phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Tạo mã QR từ ID phòng và hiển thị nó lên ImageView.
     */
    private void generateAndShowQRCode(String lobbyId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(lobbyId, BarcodeFormat.QR_CODE, 400, 400);
            imageViewQRCode.setImageBitmap(bitmap);
            textViewLobbyId.setText("Mã phòng: " + lobbyId);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cập nhật giao diện sau khi tạo phòng thành công.
     */
    private void updateUIAfterCreation() {
        buttonCreateLobby.setVisibility(View.GONE);
        layoutLobbyInfo.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);
    }

    /**
     * Đưa giao diện về trạng thái ban đầu sau khi xóa phòng.
     */
    private void resetUI() {
        currentLobbyId = null;
        layoutLobbyInfo.setVisibility(View.GONE);
        layoutControls.setVisibility(View.GONE);
        buttonCreateLobby.setVisibility(View.VISIBLE);
    }
}
