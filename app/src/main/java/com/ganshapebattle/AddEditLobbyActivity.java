package com.ganshapebattle;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner; // Import Spinner
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// THÊM CÁC DÒNG NÀY VÀO
import com.ganshapebattle.models.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.util.UUID;

public class AddEditLobbyActivity extends AppCompatActivity {

    private static final String TAG = "AddEditLobbyActivity";

    private TextView tvTitle;
    private EditText etAdmin, etMaxPlayers, etDesignTime, etVoteTime, etCreatedDate, etBeginDate;
    private Button btnSaveLobby;
    private Spinner spinnerMode, spinnerStatus; // <-- Khai báo Spinner

    private LobbyService lobbyService;
    private String currentLobbyId = null;
    private Lobby lobbyToEdit;
    private Spinner spinnerAdmin; // Thay thế EditText etAdmin
    private UserService userService; // Thêm UserService


    // Dữ liệu cho các Spinner
    private final String[] lobbyModes = {"Classic", "Timed", "Challenge", "Freestyle"};
    private final String[] lobbyStatuses = {"Waiting", "In-game", "Finished"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_lobby);

        // Ánh xạ views
        tvTitle = findViewById(R.id.tvLobbyTitle);
        spinnerAdmin = findViewById(R.id.spinnerLobbyAdmin); // Ánh xạ Spinner
        etMaxPlayers = findViewById(R.id.etLobbyMaxPlayers);
        etDesignTime = findViewById(R.id.etLobbyDesignTime);
        etVoteTime = findViewById(R.id.etLobbyVoteTime);
        btnSaveLobby = findViewById(R.id.btnSaveLobby);
        etCreatedDate = findViewById(R.id.etCreatedDate);
        etBeginDate = findViewById(R.id.etBeginDate);

        spinnerMode = findViewById(R.id.spinnerLobbyMode); // <-- Ánh xạ Spinner
        spinnerStatus = findViewById(R.id.spinnerLobbyStatus); // <-- Ánh xạ Spinner

        // Cài đặt Adapter cho các Spinner
        setupSpinners();

        lobbyService = new LobbyService();

        userService = new UserService(); // Khởi tạo UserService

        loadAdminUsernames(); // Tải danh sách admin

        currentLobbyId = getIntent().getStringExtra("LOBBY_ID_EDIT");
        if (currentLobbyId != null) {
            tvTitle.setText("Chỉnh sửa phòng chơi");
            loadLobbyDetailsForEdit(currentLobbyId);
        } else {
            tvTitle.setText("Tạo phòng chơi mới");
        }

        etCreatedDate.setOnClickListener(v -> showDatePickerDialogCreatedDate());

        etBeginDate.setOnClickListener(v -> showDatePickerDialogBeginDate());

        btnSaveLobby.setOnClickListener(v -> saveLobby());
    }
    private void loadAdminUsernames() {
        // Gọi phương thức getAllUsers có sẵn
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                // Từ danh sách User, chuyển đổi nó thành danh sách String (chỉ lấy username)
                List<String> usernames = users.stream()
                        .map(User::getUsername)
                        .collect(Collectors.toList());

                // Chạy trên UI thread để cập nhật Spinner
                runOnUiThread(() -> {
                    ArrayAdapter<String> adminAdapter = new ArrayAdapter<>(
                            AddEditLobbyActivity.this,
                            android.R.layout.simple_spinner_item,
                            usernames);
                    adminAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAdmin.setAdapter(adminAdapter);

                    // Nếu ở chế độ sửa, chọn đúng admin trong Spinner
                    if (lobbyToEdit != null) {
                        setSpinnerSelection(spinnerAdmin, usernames.toArray(new String[0]), lobbyToEdit.getAdminUsername());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải danh sách user: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditLobbyActivity.this, "Không thể tải danh sách Admin", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void setupSpinners() {
        // Adapter cho Mode Spinner
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lobbyModes);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(modeAdapter);

        // Adapter cho Status Spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lobbyStatuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
    }


    private void loadLobbyDetailsForEdit(String lobbyId) {
        lobbyService.getLobbyById(lobbyId, new SupabaseCallback<Lobby>() {
            @Override
            public void onSuccess(Lobby lobby) {
                if (lobby != null) {
                    lobbyToEdit = lobby;
                    runOnUiThread(() -> {
                        etMaxPlayers.setText(String.valueOf(lobby.getMaxPlayer()));
                        etDesignTime.setText(String.valueOf(lobby.getDesignTime()));
                        etVoteTime.setText(String.valueOf(lobby.getVoteTime()));
                        etCreatedDate.setText(lobby.getCreatedDate());
                        etBeginDate.setText(lobby.getBeginDate());

                        // Chọn giá trị đúng cho Spinner
                        setSpinnerSelection(spinnerMode, lobbyModes, lobby.getMode());
                        setSpinnerSelection(spinnerStatus, lobbyStatuses, lobby.getStatus());
                    });
                }
            }            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải chi tiết lobby: ", e);
                runOnUiThread(() -> Toast.makeText(AddEditLobbyActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // Hàm tiện ích để chọn item trong Spinner
    private void setSpinnerSelection(Spinner spinner, String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void saveLobby() {
        // Lấy admin từ Spinner thay vì EditText
        if (spinnerAdmin.getSelectedItem() == null) {
            Toast.makeText(this, "Vui lòng chọn Admin", Toast.LENGTH_SHORT).show();
            return;
        }
        String admin = spinnerAdmin.getSelectedItem().toString();

        Lobby lobbyToSave = (currentLobbyId == null) ? new Lobby() : lobbyToEdit;

        try {
            lobbyToSave.setAdminUsername(admin);
            lobbyToSave.setMode(spinnerMode.getSelectedItem().toString()); // Lấy giá trị từ Spinner
            lobbyToSave.setStatus(spinnerStatus.getSelectedItem().toString()); // Lấy giá trị từ Spinner
            lobbyToSave.setMaxPlayer(Integer.parseInt(etMaxPlayers.getText().toString()));
            lobbyToSave.setDesignTime(Integer.parseInt(etDesignTime.getText().toString()));
            lobbyToSave.setVoteTime(Integer.parseInt(etVoteTime.getText().toString()));
            lobbyToSave.setCreatedDate(etCreatedDate.getText().toString().trim());
            lobbyToSave.setBeginDate(etBeginDate.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập đúng định dạng số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLobbyId == null) {
            lobbyToSave.setId(UUID.randomUUID().toString());
        }

        executeSaveOrUpdate(lobbyToSave);
    }


    private void executeSaveOrUpdate(Lobby lobby) {
        SupabaseCallback<String> callback = new SupabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    Toast.makeText(AddEditLobbyActivity.this, result, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu lobby: ", e);
                // THÊM TOAST Ở ĐÂY
                runOnUiThread(() -> Toast.makeText(AddEditLobbyActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        };

        if (currentLobbyId == null) {
            lobbyService.insertLobby(lobby, callback);
        } else {
            lobbyService.updateLobby(currentLobbyId, lobby, callback);
        }
    }

    private void showDatePickerDialogCreatedDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    c.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etCreatedDate.setText(sdf.format(c.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showDatePickerDialogBeginDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    c.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    etBeginDate.setText(sdf.format(c.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }
}