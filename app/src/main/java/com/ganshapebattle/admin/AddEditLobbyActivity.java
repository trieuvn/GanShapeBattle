package com.ganshapebattle.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Lobby;
import com.ganshapebattle.models.User;
import com.ganshapebattle.services.LobbyService;
import com.ganshapebattle.services.SupabaseCallback;
import com.ganshapebattle.services.UserService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class AddEditLobbyActivity extends AppCompatActivity {

    private static final String TAG = "AddEditLobbyActivity";

    private TextView tvTitle;
    private EditText etMaxPlayers, etDesignTime, etVoteTime, etCreatedDate, etBeginDate;
    private Button btnSaveLobby;
    private AutoCompleteTextView spinnerMode, spinnerStatus, spinnerAdmin;

    private LobbyService lobbyService;
    private UserService userService;

    private String currentLobbyId = null;
    private Lobby lobbyToEdit;

    // Dữ liệu cho Spinner
    private final String[] lobbyModes = {"Classic", "Timed", "Challenge", "Freestyle"};
    private final String[] lobbyStatuses = {"Waiting", "In-game", "Finished"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_lobby);

        // Ánh xạ View
        tvTitle = findViewById(R.id.tvLobbyTitle);
        spinnerAdmin = findViewById(R.id.spinnerLobbyAdmin);
        spinnerMode = findViewById(R.id.spinnerLobbyMode);
        spinnerStatus = findViewById(R.id.spinnerLobbyStatus);
        etMaxPlayers = findViewById(R.id.etLobbyMaxPlayers);
        etDesignTime = findViewById(R.id.etLobbyDesignTime);
        etVoteTime = findViewById(R.id.etLobbyVoteTime);
        etCreatedDate = findViewById(R.id.etCreatedDate);
        etBeginDate = findViewById(R.id.etBeginDate);
        btnSaveLobby = findViewById(R.id.btnSaveLobby);

        // Khởi tạo service
        lobbyService = new LobbyService();
        userService = new UserService();

        // Cài đặt adapter cho Mode và Status
        setupSpinners();

        // Tải danh sách admin
        loadAdminUsernames();

        // Kiểm tra xem là tạo mới hay chỉnh sửa
        currentLobbyId = getIntent().getStringExtra("LOBBY_ID_EDIT");
        if (currentLobbyId != null) {
            tvTitle.setText("Chỉnh sửa phòng chơi");
            loadLobbyDetailsForEdit(currentLobbyId);
        } else {
            tvTitle.setText("Tạo phòng chơi mới");
        }

        // DatePicker cho CreatedDate
        etCreatedDate.setOnClickListener(v -> showDatePickerDialog(etCreatedDate));

        // DatePicker cho BeginDate
        etBeginDate.setOnClickListener(v -> showDatePickerDialog(etBeginDate));

        // Nút lưu
        btnSaveLobby.setOnClickListener(v -> saveLobby());
    }

    // ------------------------
    // Tải danh sách admin
    // ------------------------
    private void loadAdminUsernames() {
        userService.getAllUsers(new SupabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                List<String> usernames = users.stream()
                        .map(User::getUsername)
                        .collect(Collectors.toList());

                runOnUiThread(() -> {
                    ArrayAdapter<String> adminAdapter = new ArrayAdapter<>(
                            AddEditLobbyActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            usernames
                    );
                    spinnerAdmin.setAdapter(adminAdapter);

                    // Nếu đang ở chế độ sửa, chọn đúng admin
                    if (lobbyToEdit != null && lobbyToEdit.getAdminUsername() != null) {
                        spinnerAdmin.setText(lobbyToEdit.getAdminUsername(), false);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải danh sách user: ", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditLobbyActivity.this, "Không thể tải danh sách Admin", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // ------------------------
    // Cài đặt Spinner cho Mode/Status
    // ------------------------
    private void setupSpinners() {
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, lobbyModes);
        spinnerMode.setAdapter(modeAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, lobbyStatuses);
        spinnerStatus.setAdapter(statusAdapter);
    }

    // ------------------------
    // Tải chi tiết Lobby để chỉnh sửa
    // ------------------------
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
                        spinnerMode.setText(lobby.getMode(), false);
                        spinnerStatus.setText(lobby.getStatus(), false);

                        // Nếu danh sách admin đã tải thì chọn admin luôn
                        if (spinnerAdmin.getAdapter() != null) {
                            spinnerAdmin.setText(lobby.getAdminUsername(), false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi tải chi tiết lobby: ", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditLobbyActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // ------------------------
    // Hiển thị DatePickerDialog
    // ------------------------
    private void showDatePickerDialog(EditText editText) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    c.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    editText.setText(sdf.format(c.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }

    // ------------------------
    // Lưu hoặc cập nhật Lobby
    // ------------------------
    private void saveLobby() {
        String admin = spinnerAdmin.getText().toString().trim();
        if (admin.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn Admin", Toast.LENGTH_SHORT).show();
            return;
        }

        Lobby lobbyToSave = (currentLobbyId == null) ? new Lobby() : lobbyToEdit;

        try {
            lobbyToSave.setAdminUsername(admin);
            lobbyToSave.setMode(spinnerMode.getText().toString());
            lobbyToSave.setStatus(spinnerStatus.getText().toString());
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
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Lỗi khi lưu lobby: ", e);
                runOnUiThread(() ->
                        Toast.makeText(AddEditLobbyActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        };

        if (currentLobbyId == null) {
            lobbyService.insertLobby(lobby, callback);
        } else {
            lobbyService.updateLobby(currentLobbyId, lobby, callback);
        }
    }
}
