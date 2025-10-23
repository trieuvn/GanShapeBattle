package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Picture;
import com.ganshapebattle.services.PictureService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PictureCRUDActivity extends AppCompatActivity {

    private static final String TAG = "PictureCRUDActivity";

    private ListView lvPictures;
    private Button btnAddPicture;
    private SearchView searchView;
    private PictureService pictureService;
    private ArrayAdapter<String> adapter;

    private List<Picture> displayedPictureList = new ArrayList<>();
    private List<Picture> fullPictureList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditPictureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_crud);

        lvPictures = findViewById(R.id.lvPictures);
        btnAddPicture = findViewById(R.id.btnAddPicture);
        searchView = findViewById(R.id.searchViewPictures);
        pictureService = new PictureService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvPictures.setAdapter(adapter);

        loadPictures();
        setupSearch();

        lvPictures.setOnItemClickListener((parent, view, position, id) -> {
            Picture selectedPicture = displayedPictureList.get(position);
            Intent intent = new Intent(PictureCRUDActivity.this, PictureDetailActivity.class);
            intent.putExtra("PICTURE_ID", selectedPicture.getId());
            startActivity(intent);
        });

        btnAddPicture.setOnClickListener(v -> {
            Intent intent = new Intent(PictureCRUDActivity.this, AddEditPictureActivity.class);
            addEditPictureLauncher.launch(intent);
        });

        addEditPictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadPictures();
                    }
                }
        );
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPictures(newText);
                return true;
            }
        });
    }

    private void loadPictures() {
        pictureService.getAllPictures(new SupabaseCallback<List<Picture>>() {
            @Override
            public void onSuccess(List<Picture> result) {
                runOnUiThread(() -> {
                    fullPictureList.clear();
                    fullPictureList.addAll(result);
                    updateDisplayedPictures(fullPictureList);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading pictures: ", e);
                runOnUiThread(() -> Toast.makeText(PictureCRUDActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void filterPictures(String query) {
        if (query == null || query.isEmpty()) {
            updateDisplayedPictures(fullPictureList);
        } else {
            List<Picture> filteredList = fullPictureList.stream()
                    .filter(picture -> picture.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
            updateDisplayedPictures(filteredList);
        }
    }

    private void updateDisplayedPictures(List<Picture> pictures) {
        displayedPictureList.clear();
        displayedPictureList.addAll(pictures);

        List<String> pictureNames = displayedPictureList.stream()
                .map(Picture::getName)
                .collect(Collectors.toList());

        adapter.clear();
        adapter.addAll(pictureNames);
        adapter.notifyDataSetChanged();
    }
}