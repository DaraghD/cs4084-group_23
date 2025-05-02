package com.example.WeatherApp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EditFavoritesActivity extends AppCompatActivity {
    private static final String API_KEY   = "0ec2f3bfd7e39f7e44f11e00bbd31a81";
    public  static final String EXTRA_UID = "uid";

    private String currentUid;
    private OkHttpClient httpClient = new OkHttpClient();

    private AppCompatAutoCompleteTextView editCityInput;
    private Button                      addFavoriteButton;
    private LinearLayout                favoritesContainer;

    private final List<String> suggestions = new ArrayList<>();
    private ArrayAdapter<String>  suggestionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favorites);

        // Back button
        Button backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        // Wire views
        editCityInput      = findViewById(R.id.editCityInput);
        addFavoriteButton  = findViewById(R.id.addFavoriteButton);
        favoritesContainer = findViewById(R.id.favoritesContainer);

        // Get UID
        currentUid = getIntent().getStringExtra(EXTRA_UID);
        if (currentUid == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup suggestions adapter
        suggestionAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
        );
        editCityInput.setAdapter(suggestionAdapter);
        editCityInput.setThreshold(2);

        // Show drop-down on focus/click if we have data
        editCityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !suggestions.isEmpty()) {
                editCityInput.showDropDown();
            }
        });
        editCityInput.setOnClickListener(v -> {
            if (!suggestions.isEmpty()) {
                editCityInput.showDropDown();
            }
        });

        // Listen for text changes
        editCityInput.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                Log.d("EditFav", "User typed: " + q);
                if (q.length() < 2) return;
                fetchCitySuggestions(q);
            }
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
        });

        // Add favorite on button click
        addFavoriteButton.setOnClickListener(v -> {
            String city = editCityInput.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(this, "Enter a city name", Toast.LENGTH_SHORT).show();
            } else {
                addFavorite(city);
            }
        });

        // Load existing favorites
        loadFavorites();
    }

    private void fetchCitySuggestions(String query) {
        try {
            String url = "https://api.openweathermap.org/data/2.5/find"
                    + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                    + "&type=like&sort=population&cnt=5"
                    + "&appid=" + API_KEY;

            Request req = new Request.Builder().url(url).build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { /* ignore */ }

                @Override public void onResponse(Call call, Response res) throws IOException {
                    if (!res.isSuccessful()) return;

                    try {
                        JSONObject root = new JSONObject(res.body().string());
                        JSONArray list   = root.getJSONArray("list");
                        List<String> tmp = new ArrayList<>();

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject city = list.getJSONObject(i);
                            String name = city.getString("name")
                                    + ", " + city.getJSONObject("sys").getString("country");
                            tmp.add(name);
                        }

                        runOnUiThread(() -> {
                            suggestions.clear();
                            suggestions.addAll(tmp);
                            suggestionAdapter.notifyDataSetChanged();
                            Log.d("EditFav", "Got suggestions: " + suggestions);

                            // Post showDropDown so it's called after layout
                            editCityInput.post(() -> {
                                if (editCityInput.hasFocus() && !suggestions.isEmpty()) {
                                    editCityInput.showDropDown();
                                }
                            });
                        });

                    } catch (JSONException ignored) { }
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadFavorites() {
        favoritesContainer.removeAllViews();
        DatabaseReference ref = FirebaseDatabase
                .getInstance()
                .getReference("userFavorites")
                .child(currentUid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                if (!snap.exists() || !snap.hasChildren()) {
                    TextView empty = new TextView(EditFavoritesActivity.this);
                    empty.setText("No favorites yet");
                    empty.setPadding(16,16,16,16);
                    favoritesContainer.addView(empty);
                } else {
                    for (DataSnapshot child : snap.getChildren()) {
                        fetchFavoriteDetail(child.getKey());
                    }
                }
            }
            @Override public void onCancelled(DatabaseError err) {
                Toast.makeText(EditFavoritesActivity.this,
                        "Failed to load favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchFavoriteDetail(String cityId) {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?id=" + cityId
                + "&appid=" + API_KEY;

        Request req = new Request.Builder().url(url).build();
        httpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { /* ignore */ }

            @Override public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) return;
                try {
                    JSONObject j      = new JSONObject(res.body().string());
                    String cityName   = j.getString("name");

                    runOnUiThread(() -> {
                        View row = getLayoutInflater()
                                .inflate(R.layout.item_favorite_edit,
                                        favoritesContainer, false);
                        TextView nameView = row.findViewById(R.id.favoriteCityName);
                        Button removeBtn  = row.findViewById(R.id.removeFavoriteButton);
                        nameView.setText(cityName);
                        removeBtn.setOnClickListener(v -> removeFavorite(cityId));
                        favoritesContainer.addView(row);
                    });
                } catch (JSONException ignored) {}
            }
        });
    }

    private void removeFavorite(String cityId) {
        DatabaseReference favRef = FirebaseDatabase
                .getInstance()
                .getReference("userFavorites")
                .child(currentUid)
                .child(cityId);

        favRef.removeValue()
                .addOnSuccessListener(__ -> loadFavorites())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not remove", Toast.LENGTH_SHORT).show()
                );
    }

    private void addFavorite(String cityName) {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=" + URLEncoder.encode(cityName, StandardCharsets.UTF_8)
                + "&appid=" + API_KEY;

        httpClient.newCall(new Request.Builder().url(url).build())
                .enqueue(new Callback() {
                    @Override public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(EditFavoritesActivity.this,
                                        "Lookup failed", Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onResponse(Call call, Response res) throws IOException {
                        if (!res.isSuccessful()) {
                            runOnUiThread(() ->
                                    Toast.makeText(EditFavoritesActivity.this,
                                            "City not found", Toast.LENGTH_SHORT).show());
                            return;
                        }
                        try {
                            JSONObject j  = new JSONObject(res.body().string());
                            String id     = j.getString("id");
                            DatabaseReference favRef = FirebaseDatabase
                                    .getInstance()
                                    .getReference("userFavorites")
                                    .child(currentUid)
                                    .child(id);

                            favRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(DataSnapshot snap) {
                                    if (snap.exists()) {
                                        Toast.makeText(EditFavoritesActivity.this,
                                                "Already a favorite", Toast.LENGTH_SHORT).show();
                                    } else {
                                        favRef.setValue(true)
                                                .addOnSuccessListener(__ -> runOnUiThread(() -> {
                                                    editCityInput.setText("");
                                                    loadFavorites();
                                                }))
                                                .addOnFailureListener(e ->
                                                        runOnUiThread(() ->
                                                                Toast.makeText(EditFavoritesActivity.this,
                                                                        "Could not add favorite",
                                                                        Toast.LENGTH_SHORT).show()
                                                        )
                                                );
                                    }
                                }
                                @Override public void onCancelled(DatabaseError err) {}
                            });
                        } catch (JSONException ignored) {}
                    }
                });
    }
}
