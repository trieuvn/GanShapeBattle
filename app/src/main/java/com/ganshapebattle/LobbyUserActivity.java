package com.ganshapebattle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

// import io.supabase.client.SupabaseClient; // Import thư viện Supabase của bạn

public class LobbyUserActivity extends AppCompatActivity {

    // --- UI Elements ---
    private EditText editTextLobbyId;
    private Button buttonJoin, buttonLeave;
    private LinearLayout joinLobbyLayout, inLobbyLayout;
    private TextView textViewLobbyInfo;
    private ProgressBar progressBar;

    // --- Supabase & Data ---
    // private SupabaseClient supabase;
    private String currentLobbyId;
    private String currentPlayerId; // Quan trọng để biết ai sẽ rời đi

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_user);

        // Khởi tạo Supabase client của bạn
        // supabase = SupabaseClientManager.getInstance();

        bindViews();
        setupListeners();
    }
    
    /**
     * Ánh xạ các thành phần UI từ XML.
     */
    private void bindViews() {
        editTextLobbyId = findViewById(R.id.editTextLobbyId);
        buttonJoin = findViewById(R.id.buttonJoin);
        buttonLeave = findViewById(R.id.buttonLeave);
        joinLobbyLayout = findViewById(R.id.joinLobbyLayout);
        inLobbyLayout = findViewById(R.id.inLobbyLayout);
        textViewLobbyInfo = findViewById(R.id.textViewLobbyInfo);
        progressBar = findViewById(R.id.progressBar);
    }
    
    /**
     * Thiết lập các sự kiện click cho nút.
     */
    private void setupListeners() {
        buttonJoin.setOnClickListener(v -> joinLobby());
        buttonLeave.setOnClickListener(v -> leaveLobby());
    }

    /**
     * Bước 1-12: Xử lý logic khi người dùng nhấn nút "Tham gia".
     */
    private void joinLobby() {
        String lobbyId = editTextLobbyId.getText().toString().trim();

        if (TextUtils.isEmpty(lobbyId)) {
            Toast.makeText(this, "Vui lòng nhập mã phòng!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLobbyId != null) {
            Toast.makeText(this, "Bạn đã ở trong phòng khác!", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        try {
            // --- BƯỚC 3: GỌI SUPABASE ĐỂ LẤY THÔNG TIN PHÒNG (getById) ---
            // supabase.from("lobbies").select().eq("id", lobbyId).single().execute(lobbyResponse -> {
            //     // BƯỚC 5: KIỂM TRA PHÒNG CÓ TỒN TẠI VÀ CÒN CHỖ KHÔNG (checkSlot)
            //     if (!lobbyResponse.isSuccess() || isLobbyFull(lobbyResponse.getData())) {
            //         // BƯỚC 7-8: GỬI THÔNG BÁO LỖI
            //         runOnUiThread(() -> {
            //             Toast.makeText(this, "Phòng không tồn tại hoặc đã đầy!", Toast.LENGTH_LONG).show();
            //             setLoading(false);
            //         });
            //     } else {
            //         // BƯỚC 9: THÊM NGƯỜI CHƠI MỚI VÀO BẢNG PLAYER
            //         HashMap<String, Object> newPlayer = new HashMap<>();
            //         newPlayer.put("lobby_id", lobbyId);
            //         newPlayer.put("user_id", getCurrentUserId()); // Lấy ID người dùng hiện tại
            //         newPlayer.put("joined_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(new Date()));
            //
            //         supabase.from("players").insert(newPlayer).execute(playerResponse -> {
            //             if (playerResponse.isSuccess()) {
            //                  // BƯỚC 11-12: THÀNH CÔNG, CHUYỂN HƯỚNG GIAO DIỆN
            //                 currentPlayerId = playerResponse.getData().get("id").getAsString();
            //                 currentLobbyId = lobbyId;
            //                 runOnUiThread(() -> {
            //                     setLoading(false);
            //                     showInLobbyView();
            //                     Toast.makeText(this, "Tham gia thành công!", Toast.LENGTH_SHORT).show();
            //                 });
            //             } else {
            //                 runOnUiThread(() -> {
            //                     Toast.makeText(this, "Không thể tham gia phòng: " + playerResponse.getError().getMessage(), Toast.LENGTH_LONG).show();
            //                     setLoading(false);
            //                 });
            //             }
            //         });
            //     }
            // });
            
            // ---- GIẢ LẬP KHI KHÔNG CÓ SUPABASE ----
            if (lobbyId.equals("full") || lobbyId.equals("null")) { // Giả lập phòng đầy hoặc không tồn tại
                Toast.makeText(this, "Phòng không tồn tại hoặc đã đầy!", Toast.LENGTH_LONG).show();
                setLoading(false);
            } else { // Giả lập tham gia thành công
                this.currentLobbyId = lobbyId;
                this.currentPlayerId = "player_" + System.currentTimeMillis(); // ID người chơi giả
                Toast.makeText(this, "Tham gia thành công!", Toast.LENGTH_SHORT).show();
                setLoading(false);
                showInLobbyView();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tham gia phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
        }
    }

    /**
     * Bước 13-18: Xử lý logic khi người dùng nhấn nút "Rời phòng".
     */
    private void leaveLobby() {
        if (currentPlayerId == null) {
            Toast.makeText(this, "Bạn chưa ở trong phòng nào!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);

        try {
            // --- BƯỚC 15: GỌI SUPABASE ĐỂ XÓA NGƯỜI CHƠI (delete(Player)) ---
            // supabase.from("players").delete().eq("id", currentPlayerId).execute(response -> {
            //     runOnUiThread(() -> {
            //         if (response.isSuccess()) {
            //             // BƯỚC 17-18: THÀNH CÔNG, CHUYỂN HƯỚNG GIAO DIỆN
            //             Toast.makeText(this, "Đã rời phòng.", Toast.LENGTH_SHORT).show();
            //             showJoinView();
            //         } else {
            //             Toast.makeText(this, "Lỗi khi rời phòng: " + response.getError().getMessage(), Toast.LENGTH_LONG).show();
            //         }
            //         setLoading(false);
            //     });
            // });
            
            // ---- GIẢ LẬP ----
            Toast.makeText(this, "Đã rời phòng.", Toast.LENGTH_SHORT).show();
            setLoading(false);
            showJoinView();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi rời phòng: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setLoading(false);
        }
    }
    
    /**
     * Hiển thị giao diện khi đã ở trong phòng.
     */
    private void showInLobbyView() {
        joinLobbyLayout.setVisibility(View.GONE);
        inLobbyLayout.setVisibility(View.VISIBLE);
        textViewLobbyInfo.setText("Mã phòng: " + currentLobbyId);
    }

    /**
     * Reset và hiển thị lại giao diện tham gia phòng ban đầu.
     */
    private void showJoinView() {
        inLobbyLayout.setVisibility(View.GONE);
        joinLobbyLayout.setVisibility(View.VISIBLE);
        editTextLobbyId.setText("");
        this.currentLobbyId = null;
        this.currentPlayerId = null;
    }
    
    /**
     * Hiển thị hoặc ẩn ProgressBar.
     * @param isLoading Trạng thái loading.
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonJoin.setEnabled(false);
            buttonLeave.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            buttonJoin.setEnabled(true);
            buttonLeave.setEnabled(true);
        }
    }

    /**
     * Kiểm tra xem phòng có đầy không (helper method cho Supabase).
     * @param lobbyData Dữ liệu phòng từ Supabase
     * @return true nếu phòng đầy, false nếu còn chỗ
     */
    private boolean isLobbyFull(HashMap<String, Object> lobbyData) {
        // Giả sử có field max_players và current_players trong lobby data
        // int maxPlayers = (Integer) lobbyData.get("max_players");
        // int currentPlayers = (Integer) lobbyData.get("current_players");
        // return currentPlayers >= maxPlayers;
        
        // Tạm thời return false để giả lập
        return false;
    }
}