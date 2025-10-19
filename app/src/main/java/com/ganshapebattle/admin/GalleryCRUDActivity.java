package com.ganshapebattle.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.ganshapebattle.R;
import com.ganshapebattle.models.Gallery;
import com.ganshapebattle.services.GalleryService;
import com.ganshapebattle.services.SupabaseCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GalleryCRUDActivity extends AppCompatActivity {

    private ListView lvGalleries;
    private Button btnAddGallery;
    private SearchView searchView;
    private GalleryService galleryService;
    private ArrayAdapter<String> adapter;

    private List<Gallery> displayedGalleryList = new ArrayList<>();
    private List<Gallery> fullGalleryList = new ArrayList<>();

    private ActivityResultLauncher<Intent> addEditGalleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_crud);

        lvGalleries = findViewById(R.id.lvGalleries);
        btnAddGallery = findViewById(R.id.btnAddGallery);
        searchView = findViewById(R.id.searchViewGalleries);
        galleryService = new GalleryService();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvGalleries.setAdapter(adapter);

        loadGalleries();
        setupSearch();

        lvGalleries.setOnItemClickListener((parent, view, position, id) -> {
            Gallery selectedGallery = displayedGalleryList.get(position);
            Intent intent = new Intent(GalleryCRUDActivity.this, GalleryDetailActivity.class);
            intent.putExtra("GALLERY_ID", selectedGallery.getId());
            startActivity(intent);
        });

        btnAddGallery.setOnClickListener(v -> {
            Intent intent = new Intent(GalleryCRUDActivity.this, AddEditGalleryActivity.class);
            addEditGalleryLauncher.launch(intent);
        });

        addEditGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadGalleries();
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
                filterGalleries(newText);
                return true;
            }
        });
    }

    private void loadGalleries() {
        galleryService.getAllGalleries(new SupabaseCallback<List<Gallery>>() {
            @Override
            public void onSuccess(List<Gallery> result) {
                runOnUiThread(() -> {
                    fullGalleryList.clear();
                    fullGalleryList.addAll(result);
                    updateDisplayedGalleries(fullGalleryList);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("GalleryCRUD", "Error loading galleries: ", e);
            }
        });
    }

    private void filterGalleries(String query) {
        List<Gallery> filteredList = (query == null || query.isEmpty())
                ? fullGalleryList
                : fullGalleryList.stream()
                .filter(gallery -> gallery.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        updateDisplayedGalleries(filteredList);
    }

    private void updateDisplayedGalleries(List<Gallery> galleries) {
        displayedGalleryList.clear();
        displayedGalleryList.addAll(galleries);
        List<String> galleryNames = displayedGalleryList.stream()
                .map(Gallery::getName)
                .collect(Collectors.toList());
        adapter.clear();
        adapter.addAll(galleryNames);
        adapter.notifyDataSetChanged();
    }
}